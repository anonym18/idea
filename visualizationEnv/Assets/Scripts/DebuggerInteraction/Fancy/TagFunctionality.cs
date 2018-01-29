//Attached to actors for visual representation of Breakpoints
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class TagFunctionality : MonoBehaviour {
    public GameObject visualRepresentationPrefab; //Set in the inspector (arrow prefab)
    
    private GameObject visualRepresentation;

    public static int numTagged = 0;

    public bool getTag = false;//Is the actor tagged (or not)?

    public Vector3 Offset = new Vector3(0.03f, 0.3f, 0f);

    public void TagUntag(TagActorResponse tar)
    {
        if (tar.toTag)
        {
            visualRepresentation = Instantiate(visualRepresentationPrefab);

            visualRepresentation.transform.position = transform.position + Offset; //With a offset so that it is directly above

            visualRepresentation.transform.parent = transform; //Who's the daddy?

            numTagged++; //Increment number of actors tagged
        }

        else
        {
            numTagged--; //Decrement number of actors tagged
            Destroy(visualRepresentation); //Destroy the representation
        }
    }


    public void TagReached(TagReachedResponse trr)
    {
        Log newLog = new Log(1, "Breakpoint hit : " + transform.name); //Create a Log
        VisualizationHandler.Handle(newLog); //Send it to the Visualization Handler to be handled

        //Blink the visualRepresentation
        StartCoroutine(Blinker());
    }

    public IEnumerator Blinker ()
    {
        int currStep = Trace.numOfStepsElapsed;
        Vector3 origScale = visualRepresentation.transform.localScale;
        while ((currStep == Trace.numOfStepsElapsed) && numTagged > 0)
        {                                               //Extra check condition to make sure this does not continue when history is queried
            visualRepresentation.transform.localScale = new Vector3 (0f, 0f, 0f); //Make the object so small- it's invisible
            yield return new WaitForSeconds(0.75f);
            visualRepresentation.transform.localScale = origScale; //Back to original
            yield return new WaitForSeconds(0.75f);
        }
    }

    public void ClearTag()
    {
        if(getTag)
        {
            getTag = false;
            numTagged--;
            Destroy(visualRepresentation);
        }
    }

    public bool ToggleTag() //Is not used directly for impacting visual representations
    {
        if (getTag)
        {
            getTag = false;
            return false;
        }
        else
        {
            getTag = true;
            return true;
        }
    }

}
