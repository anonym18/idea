using System.Collections.Generic;
using UnityEngine;


[System.Serializable]
public class QueryResponse
{
    public string responseType;
    public virtual void HandleThis()
    { Debug.LogError("Control has passed to the base QueryResponse class"); }
}


public class ActionResponse : QueryResponse
{ 
    public int stepNum; //Additionally, the stepNumber of the dispatcher is sent
    public List<string> events;
    public List<State> states;

    public override void HandleThis()
    { Debug.Log("Receive Response class"); }
}


public class TopographyResponse : QueryResponse //Special, as this executes immediately vis a vis ActionResponse (events added to Trace)
{
    public string topographyType; //currently only "RING"
    public List<string> orderedActorIds;

    public override void HandleThis()
    { Debug.Log("Receive Response class"); }
}

public class TagActorResponse : QueryResponse
{
    public string actorId;
    public bool toTag; //True- Tag, False- Untag
}

public class TagReachedResponse : QueryResponse
{
    public string actorId; //The actor whose tag has been reached
}

public class EOTResponse : QueryResponse //Sent when trace has ended
{

}
public class SuppressActorResponse : QueryResponse //Sent to acknowledge suppression
{
    public string actorId;
    public bool toSuppress;
    public SuppressActorResponse (string id, bool tS)
    {
        toSuppress = tS;
        actorId = id;
    }
}

public class StepResponse : QueryResponse //History at step stepNum - sent by the dispatcher to convey a historical step
{
    public int stepNum;
    public List<string> events;
    public List<State> states;
}

