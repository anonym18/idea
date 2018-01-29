//Attached to Vars (G.O. in Resources)
//Idea is to use the UpdateState function

using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class ChangeVarsText : MonoBehaviour {

    TextMesh tm;
    public GameObject varsWindow;
    // Use this for initialization

    void Start () {
        if (varsWindow != null)
        {
            tm = varsWindow.GetComponent<TextMesh>();
            tm.text = "Waiting for initial state..";
        }
        else
            Debug.LogError("The vars window is undefined!");
	}
	

    public void UpdateState(State st)
    {
        tm.text = st.vars;
        Debug.Log("Successfully updated state variables");
    }
}
