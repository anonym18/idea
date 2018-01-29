using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class MarkerFunctionality : MonoBehaviour {

    public int status;
    /*
     * -1 = disabled
     * 1 = message marked
     * 2 = actor marked
     * */
    public GameObject prefabMark; //prefab reference to the visualizer

    GameObject markerInstance;
    Material markerMat; //Material attached to the visualizer

    Renderer markerRenderer; //This is responsible for showing / hiding the mesh

    public MarkerRepresentation representationHolding; //The representation currently held
    
    void Start () {
        status = -1; //initially disabled
        if (prefabMark != null)
        {
            markerInstance = Instantiate(prefabMark);
            markerInstance.transform.parent = transform; //Who's the daddy?
            markerInstance.transform.position = transform.position; //With no offset
            markerMat = markerInstance.GetComponent<MeshRenderer>().material;
            markerRenderer = markerInstance.GetComponent<Renderer>();
        }
        else
            Debug.LogError("No marker prefab attached.");
        markerRenderer.enabled = false; //corresponds to status = -1
        representationHolding = new MarkerRepresentation(); //set it to the unmarked represenation
    }
	

    public void MarkUnmarkThis() //From UserInput
    {
        //Check whether to mark
        if (status == -1)
            MarkThis();
        else
            Debug.Log("Cannot mark this actor");
    }

    private void MarkThis() //After status check
    {
        bool messagesExist = GetComponent<ActorFunctionality>().messageQueueBox.GetComponent<MessageQueueFunctionality>().messageQueue.Count > 0 ? true : false;
        if (messagesExist)
        {
            //We already know that status = -1
            
            representationHolding = Markers.AssignNewMarker(); //Get a representation
            if (representationHolding.index != -1)
            {
                status = 1; //Message is marked
                Color matColour = representationHolding.colourOfMarker;
                markerMat.color = matColour; //Set the colour
                markerRenderer.enabled = true; //Enable the renderer
            }
        }
        else
        {
            Log newLog = new Log(1, "Only Actors with messages can be marked"); //Create a Log
            VisualizationHandler.Handle(newLog); //Send it to the Visualization Handler to be handled
        }
    }

    public void MessageMark(MarkerRepresentation mr)
    {
        if(status == -1 && mr.index!=-1)
        {
            representationHolding = mr;
            
            status = 1; //Change own status
            Color matColour = representationHolding.colourOfMarker;
            matColour.a = 0.1f;
            markerMat.color = matColour; //Set the colour
            markerRenderer.enabled = true; //Enable the renderer
        }
    }

    public void Escalate() //Called when message is marked and now we want the actor marked
    {
        Debug.Log("About to escalate mark from message to actor");
        Color matColour = representationHolding.colourOfMarker;
        matColour.a = 0.5f; // Make the alpha 50%
        markerMat.color = matColour;
        status = 2; //Now the actor is marked
    }

    public void ClearMark()
    {
        representationHolding = new MarkerRepresentation();
        status = -1; //Set back to unmarked state
        markerRenderer.enabled = false;
    }
}
