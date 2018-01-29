//We have the concept of an atomic step, which is a list of events. We need to cycle through each atomic step and each event within this step.

using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class TraceImplement : MonoBehaviour {

    public static float timeDelay = 2.5f; //time delay (in addition to what happens in ImplementNext)
    public static float timeDelayForOutline;
    public static GameObject rootOfActors;
    private AudioSource audioS;
  
    void Awake()
    {
        audioS = GetComponent<AudioSource>();
        rootOfActors = GameObject.Find("RootActors"); //The parent to everything
    }
    IEnumerator Start()
    {
        while (true) //Execute indefinitely
        {
            if (Trace.stepEvents.Count == 0 && Trace.stepStates.Count == 0) //If there are no historical steps to be implemented
            {
                yield return new WaitForSeconds(timeDelay); //Time delay for each event visualization
                if (!UserInputHandler.isPaused)
                {
                    if (Trace.NewStepPossible())
                    {
                        audioS.Play(); //Play a sound
                        if (!Trace.allEvents[Trace.pointerToCurrAtomicStep][Trace.pointerToCurrEvent].isSuppressed)
                        {
                            Trace.allEvents[Trace.pointerToCurrAtomicStep][Trace.pointerToCurrEvent].HandleOutline();    //Do the outlining
                            yield return new WaitForSeconds(0.25f); //Time delay for actual visualization
                            Trace.allEvents[Trace.pointerToCurrAtomicStep][Trace.pointerToCurrEvent].HandleVisualization();
                        }
                        else
                            Trace.allEvents[Trace.pointerToCurrAtomicStep][Trace.pointerToCurrEvent].HandleDiscreetly();

                        rootOfActors.BroadcastMessage("NewTraceStep", SendMessageOptions.DontRequireReceiver);

                        Trace.IncrementPointer(); //Let's move to the next event
                    }

                }
            }
            else //There ARE historical steps to be implemented
            {
                Debug.Log("Implementing step response");
                bool prevLogDispValue = DiscreetHandler.logCreateForEvent;
                DiscreetHandler.logCreateForEvent = false; //Because it does not make sense to display logs of this
                
                //Empty all message queues
                foreach(var item in Actors.allActors)
                {
                    ActorFunctionality af = item.Value.GetComponent<ActorFunctionality>();
                    af.messageQueueBox.GetComponent<MessageQueueFunctionality>().EmptyQueue(); //Empty all actors' queues

                    //Clear all markings
                    Markers.Initialize();
                    rootOfActors.BroadcastMessage("ClearMark");

                    //Clear breakpoints
                    rootOfActors.BroadcastMessage("ClearTag");
                }

                foreach (ActorEvent ev in Trace.stepEvents) //Handle each event discreetly
                {
                    ev.HandleDiscreetly();
                }

                foreach (State st in Trace.stepStates) //Set the states now as all events have been handled
                {
                    NetworkInterface.StateUnwrapper(st);
                }

                DiscreetHandler.logCreateForEvent = prevLogDispValue;
                Trace.stepEvents = new List<ActorEvent>(); //Reset the list as everything has been implelented
            }
        }
    }
}
