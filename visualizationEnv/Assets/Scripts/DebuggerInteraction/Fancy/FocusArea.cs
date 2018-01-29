using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class FocusArea : MonoBehaviour
{
    public GameObject controllerWithLaser; //Set in the inspector
    private Vector3 scale;

    private void Awake()
    {
        if (controllerWithLaser == null)
            Debug.LogError("The controller with laser pointer isn't appropriately defined for the focus area.");
    }


    void Start()
    {
        scale = transform.localScale;


        controllerWithLaser.GetComponent<VRTK.VRTK_ControllerEvents>().TriggerClicked += new VRTK.ControllerInteractionEventHandler(Snap); //Listen to trigger event
    }


    private void Snap(object sender, VRTK.ControllerInteractionEventArgs e)
    {
        if (UserInputHandler.laserPointedActor != null) //if we are pointing at something
        {
            if (UserInputHandler.laserPointedActor.CompareTag("Actor")) //If it is an actor
            {
                Vector3 pos = UserInputHandler.laserPointedActor.position;
                if ((pos.x > (transform.position.x - scale.x / 2)) && (UserInputHandler.laserPointedActor.position.x < (transform.position.x + scale.x / 2)) && (UserInputHandler.laserPointedActor.position.z > (transform.position.z - scale.y / 2)) && (UserInputHandler.laserPointedActor.position.z < (transform.position.z + scale.y / 2))) //if it is already in focus area
                {
                    //Take the actor back to the original position
                    Debug.Log("Un-snapping " + UserInputHandler.laserPointedActor.name + " out of focus area.");
                    UserInputHandler.laserPointedActor.position = UserInputHandler.laserPointedActor.GetComponent<ActorFunctionality>().originalPosition;
                }
                else
                {//Code for snapping
                    UserInputHandler.laserPointedActor.GetComponent<ActorFunctionality>().originalPosition = UserInputHandler.laserPointedActor.position; //Reset the original position if in case the actor has been moved 
                    UserInputHandler.laserPointedActor.position = transform.position + new Vector3(Random.Range(-scale.x, scale.x) / 2, 1.7f, Random.Range(-scale.y, scale.y) / 2);
                    Debug.Log("Snapping " + UserInputHandler.laserPointedActor.name + " into focus area.");
                }
            }
        }
    }
}
