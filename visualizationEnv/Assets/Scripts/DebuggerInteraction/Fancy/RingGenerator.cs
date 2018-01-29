using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public static class RingGenerator //Generates a ring topography
{                                  //Dist from user //horizontal dist                            
    static Vector3 min = new Vector3(0.5f, 0.6f, -1.5f), max = new Vector3(1.3f, 2f, 1.5f), centre = new Vector3((min.x + max.x) / 2f, 0, (min.z + max.z) / 2f);
    static float radius = (max.z - min.z) / 2f;

    public static List<Vector3> Create(int size) //Returns list of positions
    {
        List<Vector3> positions = new List<Vector3>(size);

        for (int i =0; i < size; i++)
        {
            float clampedI = (i * 1.0f) / (size * 1.0f);
            Debug.Log(clampedI);
            float theta = clampedI * Mathf.PI * 2f;
            float y = min.y + clampedI * max.y;              
            positions.Add(new Vector3(Mathf.Sin(theta) * radius, 1.4f , Mathf.Cos(theta) * radius) + centre);
        }
        return positions;
    }
}
