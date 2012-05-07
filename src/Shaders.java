/* This file is autogenerated by build.sh */

public class Shaders {
  private Shaders() { };
  public static final String VERTEX = "#version 330\nlayout(location=0) in vec4 in_Position;layout(location=1) in vec4 in_Color;out vec4 ex_Color;uniform mat4 TransformationMatrix;void main(void){	gl_Position = TransformationMatrix * in_Position;  gl_Position.z *= 0.9f;	ex_Color = in_Color;}";
  public static final String FRAGMENT = "#version 330\nin vec4 ex_Color;out vec4 out_Color;void main(void){	out_Color = ex_Color;}";
  public static final String PHYSICS = "kernel void simulate(  global float4 *positions,  global float4 *velocities,  global float4 *colors,  long delta) {    unsigned int id = get_global_id(0);    float vel = fabs(velocities[id].x) * 2.0f + fabs(velocities[id].y);    colors[id].x = (    clamp(500.0f * vel, 0.0f, 1.0f) -    clamp(170.0f * vel, 0.0f, 1.0f)  );  colors[id].y = (    clamp(190.0f * vel, 0.0f, 1.0f) -    clamp(90.0f * vel, 0.0f, 1.0f)  );  colors[id].z = clamp(80.0f * vel, 0.2f, 1.0f) - 0.2f;    positions[id] += velocities[id] * delta;  velocities[id] += (float4)(0.0f, 1.0e-7f, 0.0f, 0.0f) * delta;    if (positions[id].y < -1.0f) {    positions[id].y = -positions[id].y;  } else if (positions[id].y > 1.0f) {    positions[id].y -= 2.0f;  } else if (positions[id].x > 1.0f) {    positions[id].x -= 2.0f;  } else if (positions[id].x < -1.0f) {    positions[id].x += 2.0f;  }}";
}
