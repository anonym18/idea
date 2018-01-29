using System.Collections;
using System.Collections.Generic;
using UnityEngine;

[System.Serializable]
public class ActorEvent
{
    public string eventType;
    public bool isSuppressed; //is its' visualization suppressed?
    public virtual void HandleVisualization() { Debug.LogError("Event parent cannot be visualized"); }
    public virtual void HandleOutline() { Debug.LogError("Event parent cannot be outlined"); }
    public virtual void HandleDiscreetly() { Debug.LogError("Event parent cannot be discreetly handled"); }
}


[System.Serializable]
public class ActorCreated : ActorEvent
{
    public string actorId;
    public string resourceId;
    public Vector3 position; //position it is at

    public override void HandleVisualization()
    { VisualizationHandler.Handle(this); }
    public override void HandleOutline()
    { VisualizationHandler.Outline(this); }

    public override void HandleDiscreetly()
    { DiscreetHandler.Handle(this); }

    public ActorCreated(string id, string modelType)
    {
        actorId = id;
        resourceId = modelType;
    }
}

[System.Serializable]
public class ActorDestroyed : ActorEvent
{
    public string actorId;
    public string resourceId; //Added to support backtracking on the dispatcher end

    public override void HandleVisualization()
    { VisualizationHandler.Handle(this); }
    public override void HandleOutline()
    { VisualizationHandler.Outline(this); }
    public override void HandleDiscreetly()
    { DiscreetHandler.Handle(this); }

}
[System.Serializable]
public class MessageSent : ActorEvent
{
    public string receiverId;
    public string senderId;
    public string msg;

    public override void HandleVisualization() { VisualizationHandler.Handle(this); }
    public override void HandleOutline()
    { VisualizationHandler.Outline(this); }
    public override void HandleDiscreetly()
    { DiscreetHandler.Handle(this); }

}

[System.Serializable]
public class MessageReceived : ActorEvent
{
    public string receiverId;
    public string senderId;
    public string msg;

    public override void HandleVisualization() { VisualizationHandler.Handle(this); }
    public override void HandleOutline()
    { VisualizationHandler.Outline(this); }
    public override void HandleDiscreetly()
    { DiscreetHandler.Handle(this); }

}

[System.Serializable]
public class Log : ActorEvent
{
    public int logType;
    /*
     * 0 = Debug
     * 1 = Info
     * 2 = Warning
     * 3 = Error
     * 
     */
    public string text; //To be displayed on the big screen
    public override void HandleVisualization()
    {
        VisualizationHandler.Handle(this);
    }
    public override void HandleOutline()
    { VisualizationHandler.Outline(this); }

    public override void HandleDiscreetly()
    { VisualizationHandler.Handle(this); }

    public Log (int type, string desc)
    {
        logType = type;
        text = desc;
    }
}

public class MessageDropped : ActorEvent
{
    public string receiverId;
    public string senderId;
    public string msg;

    public override void HandleVisualization() { VisualizationHandler.Handle(this); }
    public override void HandleOutline()
    { VisualizationHandler.Outline(this); }
    public override void HandleDiscreetly()
    { VisualizationHandler.Handle(this); }

}
//State class encompases variable states and behaviour states as text / colour
[System.Serializable]
public class State
{
    public string actorId;
    public Color behavior;
    public string vars; //All variables separated by new line
}

