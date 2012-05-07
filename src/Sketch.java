import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.PixelFormat;

import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.Util;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL10GL.*;

public class Sketch {
  private static final int NUMBER_OF_POINTS = 1000000;
  private static final int WIDTH = 600, HEIGHT = 600;
  
  private Random random;
  private FloatBuffer vertices;
  private FloatBuffer colors;
  private FloatBuffer velocities;
  private CLKernel simulationKernel;
  private CLCommandQueue processorCommandsQueue;
  private CLMem sharedVertexMemory;
  private CLMem sharedColorMemory;
  private long lastFrameDeltaInMillis;
  private Matrix4f worldTransformation;
  private int indexOfTransformUniform;
  
  protected void initializeGeometry() {
    for (int i = 0; i < NUMBER_OF_POINTS; i++) {
      vertices.put(random.nextFloat() * 1 - 0.5f);
      vertices.put(random.nextFloat() * 1 - 0.5f);
      vertices.put(0);
      vertices.put(1.0f);
      
      velocities.put((random.nextFloat() - 0.5f) * 1e-3f);
      velocities.put(random.nextFloat() * 1e-3f);
      velocities.put(0);
      velocities.put(0.0f);
      
      colors.put(0f);
      colors.put(0f);
      colors.put(0f);
      colors.put(random.nextFloat() * 0.5f + 0.5f);
    }
    
    colors.rewind();
    vertices.rewind();
    velocities.rewind();
  }
  
  protected void setupBuffers() throws LWJGLException {
    int vertexArrayID = glGenVertexArrays();
    glBindVertexArray(vertexArrayID);
    
    int vertexBufferID = glGenBuffers();
    glBindBuffer(GL_ARRAY_BUFFER, vertexBufferID);
    glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
    glVertexAttribPointer(0, 4, GL_FLOAT, false, 0, 0);
    glEnableVertexAttribArray(0);
    
    int colorBufferID = glGenBuffers();
    glBindBuffer(GL_ARRAY_BUFFER, colorBufferID);
    glBufferData(GL_ARRAY_BUFFER, colors, GL_STATIC_DRAW);
    glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, 0);
    glEnableVertexAttribArray(1);
    
    CLPlatform platform = CLPlatform.getPlatforms().get(0);
    List<CLDevice> devices = platform.getDevices(CL_DEVICE_TYPE_GPU);
    CLContext context = CLContext.create(platform, devices, null, Display.getDrawable(), null);
    
    CLCommandQueue queue = processorCommandsQueue = clCreateCommandQueue (
      context, devices.get(0), CL_QUEUE_PROFILING_ENABLE, null);
    
    CLMem positionsForCL = sharedVertexMemory = clCreateFromGLBuffer (
      context, CL_MEM_READ_WRITE, vertexBufferID, null);
    
    CLMem velocitiesForCL = clCreateBuffer (
      context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, velocities, null);
    
    CLMem colorsForCL = sharedColorMemory = clCreateFromGLBuffer (
      context, CL_MEM_READ_WRITE, colorBufferID, null);
    
    clFinish(queue);
    
    CLProgram program = clCreateProgramWithSource(context, Shaders.PHYSICS, null);
    Util.checkCLError(clBuildProgram(program, devices.get(0), "", null));
    
    CLKernel kernel = simulationKernel = clCreateKernel(program, "simulate", null);
    kernel.setArg(0, positionsForCL);
    kernel.setArg(1, velocitiesForCL);
    kernel.setArg(2, colorsForCL);
    kernel.setArg(3, 0);
  }
  
  protected void setupShaders() {
    int vertexShaderID = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vertexShaderID, Shaders.VERTEX);
    glCompileShader(vertexShaderID);
    
    String vertexShaderError = glGetShaderInfoLog(vertexShaderID, 1024);
    
    if (vertexShaderError.length() != 0)
      System.err.println("Vertex Shader Error: " + vertexShaderError);
    
    int fragmentShaderID = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fragmentShaderID, Shaders.FRAGMENT);
    glCompileShader(fragmentShaderID);
    
    String fragmentShaderError = glGetShaderInfoLog(fragmentShaderID, 1024);
    
    if (fragmentShaderError.length() != 0)
      System.err.println("Fragment Shader Error: " + fragmentShaderError);
    
    int programID = glCreateProgram();
    glAttachShader(programID, vertexShaderID);
    glAttachShader(programID, fragmentShaderID);
    glLinkProgram(programID);
    
    String entireProgramError = glGetProgramInfoLog(programID, 1024);
    
    if (entireProgramError.length() != 0)
      System.err.println("Program Error: " + entireProgramError);
    
    glValidateProgram(programID);
    glUseProgram(programID);
    
    int index = indexOfTransformUniform = glGetUniformLocation (
      programID, "TransformationMatrix");
  }
  
  protected void setupGL() throws LWJGLException {
    setupBuffers();
    setupShaders();
    
    glPointSize(4.0f);
    glViewport(0, 0, WIDTH, HEIGHT);
    glClearColor(0.1f, 0.1f, 0.1f, 0.0f);
  }
  
  protected void update() {
    /* worldTransformation.rotate(0.001f * lastFrameDeltaInMillis, new Vector3f(0f, 1f, 0f)); */
    
    PointerBuffer workDimensions = BufferUtils.createPointerBuffer(1);
    workDimensions.put(0, NUMBER_OF_POINTS);
    
    simulationKernel.setArg(3, lastFrameDeltaInMillis);
    
    clFinish(processorCommandsQueue);
    clEnqueueAcquireGLObjects(processorCommandsQueue, sharedVertexMemory, null, null);
    clEnqueueAcquireGLObjects(processorCommandsQueue, sharedColorMemory, null, null);
    clEnqueueNDRangeKernel (processorCommandsQueue, simulationKernel,
      1, null, workDimensions, null, null, null);
    clEnqueueReleaseGLObjects(processorCommandsQueue, sharedColorMemory, null, null);
    clEnqueueReleaseGLObjects(processorCommandsQueue, sharedVertexMemory, null, null);
    clFinish(processorCommandsQueue);
  }
  
  protected void render() {
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    
    FloatBuffer storedTransformation = BufferUtils.createFloatBuffer(16);
    worldTransformation.store(storedTransformation);
    storedTransformation.rewind();
    
    glUniformMatrix4(indexOfTransformUniform, false, storedTransformation);
    
    glDrawArrays(GL_POINTS, 0, NUMBER_OF_POINTS);
  }
  
  public void launch() throws Throwable {
    Display.setTitle("Pixel Flame");
    Display.setDisplayMode(new DisplayMode(HEIGHT, WIDTH));
    Display.create(new PixelFormat().withAlphaBits(8).withDepthBits(24),
      new ContextAttribs(3, 2).withProfileCore(true));
    
    CL.create();
    
    setupGL();
    
    long previousTime = System.currentTimeMillis();
    
    while (!Display.isCloseRequested()) {
      long currentTime = System.currentTimeMillis();
      long delta = lastFrameDeltaInMillis = currentTime - previousTime + 1;
      
      update();
      render();
      
      Display.update();
      
      previousTime = currentTime;
    }
    
    Display.destroy();
  }
  
  public Sketch() {
    vertices = BufferUtils.createFloatBuffer(NUMBER_OF_POINTS * 4);
    velocities = BufferUtils.createFloatBuffer(NUMBER_OF_POINTS * 4);
    colors = BufferUtils.createFloatBuffer(NUMBER_OF_POINTS * 4);
    random = new Random();
    worldTransformation = new Matrix4f();
    
    initializeGeometry();
  }
  
  public static void main(String[] args) throws Throwable {
    new Sketch().launch();
  }
}