//Initializes the visualization and starts the TCP chain

using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Init : MonoBehaviour {
    public bool isActive;
	// Use this for initialization
	void Start () {
        if (isActive)
        {
            ActorCreated deadLetters = new ActorCreated("deadLetters", "Postkasten"); //Initiallization of initial actor
            List<ActorEvent> tempList = new List<ActorEvent>(); //Make a temporary list to hold this
            tempList.Add(deadLetters);
            Trace.allEvents.Add(tempList);

            Debug.Log("About to start the Network Interface..");
            AsynchronousClient.StartClient();
        }
    }

    private void OnApplicationQuit()
    {
        AsynchronousClient.FreeSocket(); //Free socket after everything
    }


}
