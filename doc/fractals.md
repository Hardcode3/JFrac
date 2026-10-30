# Fractals

## Julia vs Mandelbrot sets

Julia and Mandelbrot share the same root equation $z_{n+1} = z_n^2 + c$.
However, they 'invert' the roles of $z_0$ and $c$.

Here is a summary:

| Characteristic               | Mandelbrot Set                                                | A Julia Set                                                            |
| :--------------------------- | :------------------------------------------------------------ | :--------------------------------------------------------------------- |
| **Iterated Equation**        | $z_{n+1} = z_n^2 + c$                                         | $z_{n+1} = z_n^2 + c$                                                  |
| **Starting Point ($z_{0}$)** | Fixed and null ($z_0 = 0$)                                    | Variable (corresponds to the coordinates of the tested pixel)          |
| **Parameter ($c$)**          | Variable (corresponds to the coordinates of the tested pixel) | Fixed and constant (chosen at the start for the entire drawing)        |
| **Number of Images**         | There is only one unique Mandelbrot set.                      | There is an infinite number of Julia sets (one for each value of $c$). |
