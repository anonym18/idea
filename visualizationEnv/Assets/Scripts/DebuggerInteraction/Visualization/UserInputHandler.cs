using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class UserInputHandler : MonoBehaviour
{
    public static Transform laserPointedActor; //set by SteamVR_LaserPointer and accessed by NetworkInterface during sends
    public static Transform laserPointedCollider;
    public static bool isPaused;
    private bool isOn;

    private int indexAtNext = -1; //Index of the atomic step (set when going forward in a trace)

    private float counterOnOff = 0f; //Used for long holds

    private void Start()
    {
        isPaused = false;
        isOn = false;
    }

    private string GetWordForNext() //This resolves the keyword we need to send for next step
    {
        return "__NEXT__";
    }
    public void TagUntagActor()
    {
        if (CheckLaserPointer())
        {
            Debug.Log("About to tag/untag actor");

            bool toggle = laserPointedActor.gameObject.GetComponent<TagFunctionality>().ToggleTag();
            NetworkInterface.HandleTagUntagRequestToBeSent(toggle, laserPointedActor.name);
        }
    }

    public void MarkUpcomingMessage() //Marks the upcoming message with a new marker. This helps in visualizing in different colours events that happen in response to this
    {
        if (CheckLaserPointer())
        {
            Debug.Log("About to mark an upcoming message");
            laserPointedActor.GetComponent<MarkerFunctionality>().MarkUnmarkThis();

        }
    }
    public void ClearMarkings()
    {
        //Clear all markings and re-init
        Debug.Log("About to clear all markings");
        Markers.Initialize();
        TraceImplement.rootOfActors.BroadcastMessage("ClearMark");
    }

    public void ReceiveFromActor()
    {
        if (CheckAtomicStepIndex())
        {
            if (CheckLaserPointer())
            {

                Debug.Log("About to receive from actor");
                ActionRequest rr = new ActionRequest(GetWordForNext(), laserPointedActor.name);
                NetworkInterface.HandleRequest(rr);
                
            }
        }
        else
        {
            //Do nothing
        }
    }

    public void DropFromActor()
    {
        if (CheckAtomicStepIndex())
        {
            if (CheckLaserPointer())
            {
        
                Debug.Log("About to drop from actor");
                ActionRequest rr = new ActionRequest("__DROP__", laserPointedActor.name);
                NetworkInterface.HandleRequest(rr);
           
            }
        }
        else
        {
            //Do nothing
        }
    }

    public void QueryState()
    {
        if (CheckLaserPointer() )
        {
            bool toggle = laserPointedActor.gameObject.GetComponent<ActorFunctionality>().ToggleState();

            //Now, we also show the message queue box information
            GameObject mbox = laserPointedActor.gameObject.GetComponent<ActorFunctionality>().messageQueueBox;
            MessageQueueFunctionality mQueueFunc = mbox.GetComponent<MessageQueueFunctionality>();
            mQueueFunc.ToggleMsgQueueInfo();

            StateRequest sr = new StateRequest(laserPointedActor.name, toggle);
            NetworkInterface.HandleRequest(sr);
        }

    }
    public void PausePlay() 
    {
        isPaused = isPaused ? false : true;
        Debug.Log("Are we Paused? "+isPaused.ToString());
    }

    public void GoFaster()
    {
        SpeedControl.IncreaseSpeed();
    }

    public void GoSlower()
    {
        SpeedControl.DecreaseSpeed();
    }

    public void NextStep() //Go to the next step
    {
        if (CheckAtomicStepIndex())
        {
            //Send the next message to dispatcher
            ActionRequest ar = new ActionRequest(GetWordForNext(), "");
            NetworkInterface.HandleRequest(ar);
            Debug.Log("About to ask for the next step");
        }
        else
        {
            //Let the user know he / she is not ready
        }
    }

    public void QueryTopography() //Ask dispatcher what kind of topography it wants
    {
        
        TopographyRequest tr = new TopographyRequest();
        NetworkInterface.HandleRequest(tr);
        Debug.Log("About to ask for topography");
    }

    public void OnOff() //Send an init message and start the whole charade
    {
        if (!isOn)
        {
            AsynchronousClient.SendInitMessage();
            isOn = true; //No check for actual successful init
        }
    }

    public void OnOffHeld() //Long hold on the OnOff button
    {
        counterOnOff += Time.deltaTime;
        if (counterOnOff > 1.0f)
        {
            StartCoroutine(ApplicationExit());
        }
    }
    public void OnOffReset() //Resets the counter
    {
        counterOnOff = 0f;
    }

    IEnumerator ApplicationExit()
    {
        yield return new WaitForSeconds(1.0f);
        Application.Quit();
    }

    public void SuppressActor()
    {
        if (CheckLaserPointer())
        {
            bool toSuppress = laserPointedActor.GetComponent<Suppression>().isSuprressed ? false : true;
            //Suppress Visualization of the laserPointedActor
            SuppressActorRequest sar = new SuppressActorRequest(laserPointedActor.name, toSuppress);
            NetworkInterface.HandleRequest(sar);
        }
    }
    
    //Step request directly sent to Network Interface

    public static bool CheckAtomicStepIndex() //Are we up to pace on the atomic step level
    {
        if (!Trace.NewStepPossible())
            return true;
        else
            return false;
    }

    public static bool CheckLaserPointer() //Are we poinitng at an actor
    {
        if (laserPointedActor != null && laserPointedActor.CompareTag("Actor"))
            return true;
        else
            return false;
    }

}