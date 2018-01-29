//Built after read
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public static class Trace
{
    public static List<List<ActorEvent>> allEvents = new List<List<ActorEvent>>(); //List of atomic visualization steps (not same as dispatcher)

    public static List<ActorEvent> stepEvents = new List<ActorEvent>(); //List of steps received via StepResponse -> Must be empty when not implementing a step response
    public static List<State> stepStates = new List<State>();

    public static Dictionary<int, int> visualizationToDispatcherIndexMapper = new Dictionary<int, int>(); //Maps visualization step to dispatcher step
    
    //public static List<ActorEvent> currBatch = new List<ActorEvent>();
    public static int pointerToCurrEvent = 0; //Points to the currently active event
    public static int pointerToCurrAtomicStep = 0; //Pointer to the current atomic step
    public static int numOfStepsElapsed = 0;

    public static bool NewStepPossible()
    {
        if (pointerToCurrAtomicStep < allEvents.Count)
            return true;
        else
            return false;
    }

    public static void IncrementPointer() //Used to safely increment pointer
    {
        if (NewStepPossible()) //Check to avoid possible problems
        {
            if (pointerToCurrEvent < allEvents[pointerToCurrAtomicStep].Count)
            {
                pointerToCurrEvent++;
            }
            if (pointerToCurrEvent == allEvents[pointerToCurrAtomicStep].Count)
            {
                pointerToCurrAtomicStep++; //Increment 1st dimension pointer
                pointerToCurrEvent = 0; //Reset the 2nd dimension pointer
            }
            numOfStepsElapsed++; //A new step has elapsed
            Debug.Log("Incremented to Trace pointer " + pointerToCurrAtomicStep.ToString() + ", " + pointerToCurrEvent.ToString());
        }
        else
            Debug.LogError("Failed to increment Trace pointer(s)");


    }

}
