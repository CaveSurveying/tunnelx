////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1998.  
// shared with version 1
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

//
//
// Quaternion
//
//
class Quaternion 
{
	public float w = 1.0F; 
	public float x = 0.0F; 
	public float y = 0.0F; 
	public float z = 0.0F; 

	/////////////////////////////////////////////
	void SetXYZ(float lx, float ly, float lz)
	{
		x = lx; 
		y = ly; 
		z = lz; 
		w = (float)Math.sqrt(1.0F - (x * x + y * y + z * z)); 
	}

	/////////////////////////////////////////////
	void unit()
	{
		w = 1.0F; 
		x = 0.0F; 
		y = 0.0F; 
		z = 0.0F; 
	}


	/////////////////////////////////////////////
	void Mult(Quaternion R, Quaternion L)
	{
		w = L.w * R.w - L.x * R.x - L.y * R.y - L.z * R.z; 
		x = L.w * R.x + L.x * R.w + L.y * R.z - L.z * R.y; 
		y = L.w * R.y + L.y * R.w + L.z * R.x - L.x * R.z; 
		z = L.w * R.z + L.z * R.w + L.x * R.y - L.y * R.x; 
	}

	/////////////////////////////////////////////
	void VecRot(Vec3 f, Vec3 t)
	{
		x = f.y * t.z - f.z * t.y; 
		y = f.z * t.x - f.x * t.z; 
		z = f.x * t.y - f.y * t.x; 
		w = f.x * t.x + f.y * t.y + f.z * t.z; 
	}


	/////////////////////////////////////////////
	void SetFrom(Quaternion q)
	{
		x = q.x;
		y = q.y; 
		z = q.z; 
		w = q.w; 
	}
};

