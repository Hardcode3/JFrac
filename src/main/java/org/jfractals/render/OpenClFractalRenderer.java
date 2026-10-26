package org.jfractals.render;

import org.jocl.CL;
import org.jocl.CLException;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;
import org.jfractals.model.FractalType;
import org.jfractals.model.RenderSettings;

import java.math.BigDecimal;
import java.util.concurrent.CancellationException;

import static org.jocl.CL.CL_DEVICE_NOT_FOUND;
import static org.jocl.CL.CL_DEVICE_DOUBLE_FP_CONFIG;
import static org.jocl.CL.CL_DEVICE_MAX_CLOCK_FREQUENCY;
import static org.jocl.CL.CL_DEVICE_MAX_COMPUTE_UNITS;
import static org.jocl.CL.CL_DEVICE_TYPE_GPU;
import static org.jocl.CL.CL_MEM_WRITE_ONLY;
import static org.jocl.CL.CL_SUCCESS;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetDeviceInfo;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clReleaseProgram;
import static org.jocl.CL.clSetKernelArg;

/** Calculates pixels with OpenCL 64-bit floating-point arithmetic. */
public final class OpenClFractalRenderer implements AutoCloseable {
    private static final String KERNEL_SOURCE = """
            #ifdef cl_khr_fp64
            #pragma OPENCL EXTENSION cl_khr_fp64 : enable
            #elif defined(cl_amd_fp64)
            #pragma OPENCL EXTENSION cl_amd_fp64 : enable
            #else
            #error This device does not support OpenCL double precision
            #endif

            double2 complexPower(double2 value, double exponent) {
                double magnitude = length(value);
                if (magnitude == 0.0) {
                    return (double2)(0.0, 0.0);
                }
                double angle = atan2(value.y, value.x) * exponent;
                double poweredMagnitude = pow(magnitude, exponent);
                return (double2)(poweredMagnitude * cos(angle), poweredMagnitude * sin(angle));
            }

            __kernel void renderFractal(
                    __global uint* pixels,
                    int width,
                    int height,
                    double xMin,
                    double yMin,
                    double xRange,
                    double viewHeight,
                    int maxIterations,
                    double escapeRadius,
                    int smooth,
                    double exponent,
                    double juliaReal,
                    double juliaImag,
                    int fractalType) {
                size_t x = get_global_id(0);
                size_t y = get_global_id(1);
                if (x >= (size_t)width || y >= (size_t)height) {
                    return;
                }

                double real = fma((double)x / (double)width, xRange, xMin);
                double imaginary = fma((double)y / (double)height, viewHeight, yMin);
                double2 point = (double2)(real, imaginary);
                double2 c = fractalType == 0 ? point : (double2)(juliaReal, juliaImag);
                double2 z = fractalType == 0 ? (double2)(0.0, 0.0) : point;
                double escapedAt = (double)maxIterations;
                double escapeRadiusSquared = escapeRadius * escapeRadius;

                for (int iteration = 0; iteration < maxIterations; iteration++) {
                    if (z.x == 0.0 && z.y == 0.0) {
                        z = c;
                    } else if (exponent == 2.0) {
                        double nextReal = fma(-z.y, z.y, c.x);
                        nextReal = fma(z.x, z.x, nextReal);
                        double nextImaginary = fma(2.0 * z.x, z.y, c.y);
                        z = (double2)(nextReal, nextImaginary);
                    } else {
                        z = complexPower(z, exponent) + c;
                    }

                    double magnitudeSquared = fma(z.x, z.x, z.y * z.y);
                    if (magnitudeSquared > escapeRadiusSquared) {
                        escapedAt = (double)iteration;
                        if (smooth != 0 && exponent != 1.0) {
                            double magnitude = sqrt(magnitudeSquared);
                            double logMagnitude = log(magnitude);
                            if (logMagnitude > 1.0 && isfinite(logMagnitude)) {
                                escapedAt = (double)iteration + 1.0
                                        - log(logMagnitude) / log(exponent);
                            }
                        }
                        break;
                    }
                }

                double normalized = escapedAt / (double)maxIterations;
                if (!isfinite(normalized)) {
                    normalized = 0.0;
                }
                normalized = clamp(normalized, 0.0, 1.0);
                uint gray = convert_uint_sat_rte(normalized * 255.0);
                size_t index = y * (size_t)width + x;
                pixels[index] = 0xff000000u | (gray << 16) | (gray << 8) | gray;
            }
            """;

    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_program program;
    private cl_kernel kernel;

    /** Called by the render service's single background worker. */
    public int[] render(int width, int height, BigDecimal viewHeight,
                        BigDecimal centerX, BigDecimal centerY, RenderSettings settings) {
        initializeIfNeeded();
        int pixelCount = Math.multiplyExact(width, height);
        int[] pixels = new int[pixelCount];
        double viewHeightDouble = viewHeight.doubleValue();
        double xRange = viewHeightDouble * ((double) width / height);
        double xMin = centerX.doubleValue() - xRange / 2.0;
        double yMin = centerY.doubleValue() - viewHeightDouble / 2.0;

        cl_mem output;
        try {
            output = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                    (long) Sizeof.cl_uint * pixelCount, null, null);
        } catch (CLException exception) {
            throw new GpuFallbackException("OpenCL could not allocate the GPU output buffer", exception);
        }
        try {
            setMemoryArgument(0, output);
            setIntegerArgument(1, width);
            setIntegerArgument(2, height);
            setDoubleArgument(3, xMin);
            setDoubleArgument(4, yMin);
            setDoubleArgument(5, xRange);
            setDoubleArgument(6, viewHeightDouble);
            setIntegerArgument(7, settings.maxIterations());
            setDoubleArgument(8, settings.escapeRadius());
            setIntegerArgument(9, settings.smooth() ? 1 : 0);
            setDoubleArgument(10, settings.exponent());
            setDoubleArgument(11, settings.juliaReal());
            setDoubleArgument(12, settings.juliaImag());
            setIntegerArgument(13, settings.type() == FractalType.MANDELBROT ? 0 : 1);

            clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                    new long[]{width, height}, null, 0, null, null);
            clEnqueueReadBuffer(commandQueue, output, CL_TRUE, 0,
                    (long) Sizeof.cl_uint * pixelCount, Pointer.to(pixels), 0, null, null);
        } catch (CLException exception) {
            throw new GpuFallbackException("OpenCL could not calculate this image", exception);
        } finally {
            clReleaseMemObject(output);
        }
        return pixels;
    }

    private void initializeIfNeeded() {
        if (kernel != null) {
            return;
        }

        try {
            CL.setExceptionsEnabled(true);
            cl_device_id device = findGpuDevice();
            context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
            commandQueue = clCreateCommandQueue(context, device, 0, null);
            program = clCreateProgramWithSource(context, 1, new String[]{KERNEL_SOURCE}, null, null);
            clBuildProgram(program, 0, null, null, null, null);
            kernel = clCreateKernel(program, "renderFractal", null);
        } catch (GpuFallbackException exception) {
            releaseResources();
            throw exception;
        } catch (RuntimeException | LinkageError exception) {
            releaseResources();
            throw new GpuFallbackException(
                    "No usable OpenCL GPU with 64-bit floating-point support was found. "
                            + "Check that the GPU driver and OpenCL runtime are installed.",
                    exception);
        }
    }

    private static cl_device_id findGpuDevice() {
        CL.setExceptionsEnabled(false);
        try {
            int[] platformCount = new int[1];
            int result = clGetPlatformIDs(0, null, platformCount);
            if (result != CL_SUCCESS || platformCount[0] == 0) {
                throw unavailable("No OpenCL platform was found");
            }

            cl_platform_id[] platforms = new cl_platform_id[platformCount[0]];
            result = clGetPlatformIDs(platforms.length, platforms, null);
            if (result != CL_SUCCESS) {
                throw unavailable("Could not enumerate OpenCL platforms");
            }

            cl_device_id bestDevice = null;
            long bestScore = -1;
            for (cl_platform_id platform : platforms) {
                int[] deviceCount = new int[1];
                result = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 0, null, deviceCount);
                if (result == CL_DEVICE_NOT_FOUND || deviceCount[0] == 0) {
                    continue;
                }
                if (result != CL_SUCCESS) {
                    continue;
                }
                cl_device_id[] devices = new cl_device_id[deviceCount[0]];
                result = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devices.length, devices, null);
                if (result != CL_SUCCESS) {
                    continue;
                }
                for (cl_device_id device : devices) {
                    long[] doubleFpConfig = new long[1];
                    result = clGetDeviceInfo(device, CL_DEVICE_DOUBLE_FP_CONFIG, Sizeof.cl_ulong,
                            Pointer.to(doubleFpConfig), null);
                    if (result != CL_SUCCESS || doubleFpConfig[0] == 0) {
                        continue;
                    }

                    int[] computeUnits = new int[1];
                    int[] clockFrequency = new int[1];
                    int computeResult = clGetDeviceInfo(device, CL_DEVICE_MAX_COMPUTE_UNITS,
                            Sizeof.cl_uint, Pointer.to(computeUnits), null);
                    int clockResult = clGetDeviceInfo(device, CL_DEVICE_MAX_CLOCK_FREQUENCY,
                            Sizeof.cl_uint, Pointer.to(clockFrequency), null);
                    if (computeResult != CL_SUCCESS || clockResult != CL_SUCCESS) {
                        continue;
                    }

                    long score = (long) computeUnits[0] * clockFrequency[0];
                    if (score > bestScore) {
                        bestDevice = device;
                        bestScore = score;
                    }
                }
            }
            if (bestDevice != null) {
                return bestDevice;
            }
            throw unavailable("No OpenCL-compatible GPU was found");
        } finally {
            CL.setExceptionsEnabled(true);
        }
    }

    private static GpuFallbackException unavailable(String reason) {
        return new GpuFallbackException(
                reason + ". Check that the GPU driver and OpenCL runtime are installed.");
    }

    private void setMemoryArgument(int index, cl_mem value) {
        clSetKernelArg(kernel, index, Sizeof.cl_mem, Pointer.to(value));
    }

    private void setIntegerArgument(int index, int value) {
        clSetKernelArg(kernel, index, Sizeof.cl_int, Pointer.to(new int[]{value}));
    }

    private void setDoubleArgument(int index, double value) {
        clSetKernelArg(kernel, index, Sizeof.cl_double, Pointer.to(new double[]{value}));
    }

    @Override
    public void close() {
        releaseResources();
    }

    private void releaseResources() {
        release(() -> {
            if (kernel != null) clReleaseKernel(kernel);
        });
        kernel = null;
        release(() -> {
            if (program != null) clReleaseProgram(program);
        });
        program = null;
        release(() -> {
            if (commandQueue != null) clReleaseCommandQueue(commandQueue);
        });
        commandQueue = null;
        release(() -> {
            if (context != null) clReleaseContext(context);
        });
        context = null;
    }

    private static void release(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | LinkageError ignored) {
            // Keep releasing the remaining OpenCL resources after a cleanup error.
        }
    }
}
