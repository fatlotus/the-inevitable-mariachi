# Doing Advanced Particle Motion Properly

This project demonstrates proper OpenGL 3.2 custom shader support combined with the OpenCL/OpenGL "interop" functionality. Though there are few comments, the code has been designed to be relatively easy to use and understand, and largely portable. Aside from a few platform-specific issues (chiefly among them being the hardcoded `$PLATFORM` variable in `run.sh`) the code should work on most platforms.

Perhaps the only "gotcha" is the auto-generated source file `src/Shaders.java`. Perhaps impulsively, the code ends up constructing it in a shell script that then compiles it into a Java class. The reason for this was simplicity: the entire project can be contained in several JARs and a collection of native libraries. Compilation on non-POSIX platforms might get tricky, however...

In terms of licensing, all files in `lib/` and `ext/` are covered under the LWJGL license in `doc/LWJGL_License.txt`. All files included in `src/`, and `classes/` are under the following license. Naturally, if another license is preferable, then naturally I am open to suggestions.

## License

> Copyright (c) 2012 Jeremy Archer
> 
> Permission is hereby granted, free of charge, to any
> person obtaining a copy of this software and associated
> documentation files (the "Software"), to deal in the
> Software without restriction, including without
> limitation the rights to use, copy, modify, merge,
> publish, distribute, sublicense, and/or sell copies of
> the Software, and to permit persons to whom the Software
> is furnished to do so, subject to the following
> conditions:
> 
> The above copyright notice and this permission notice
> shall be included in all copies or substantial portions
> of the Software.
> 
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF
> ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
> TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
> PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
> SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
> CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
> OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
> IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
> DEALINGS IN THE SOFTWARE.