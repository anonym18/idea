using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class DestroyArcAfterSteps : MonoBehaviour {


    private int traceCounterInitially;
    public int stepsBeforeDeath = 3; //Steps after which this should die

	// Use this for initialization
	void Start () {
        traceCounterInitially = Trace.pointerToCurrEvent;
	}
	
	// Update is called once per frame
	void Update ()
    {
		if(Trace.pointerToCurrEvent == (stepsBeforeDeath + traceCounterInitially))
        {
            Destroy(gameObject);
        }
	}
}
