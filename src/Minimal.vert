#version 330\n

layout(location=0) in vec4 in_Position;
layout(location=1) in vec4 in_Color;
out vec4 ex_Color;

uniform mat4 TransformationMatrix;

void main(void)
{
	gl_Position = TransformationMatrix * in_Position;
  gl_Position.z *= 0.9f;
	ex_Color = in_Color;
}