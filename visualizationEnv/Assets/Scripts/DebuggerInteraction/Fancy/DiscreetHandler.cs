using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System.Linq;

public class DiscreetHandler : MonoBehaviour {
    public static bool logCreateForEvent = true;

    public static void Handle(ActorCreated currEvent)
    {
        GameObject go;
        if (currEvent.resourceId == "" || currEvent.resourceId == null)
            go = Instantiate(VisualizationHandler.modelDictionary["Cube"]); //If type is not set, we want a cube
        else
            go = Instantiate(VisualizationHandler.modelDictionary[currEvent.resourceId]);

        go.transform.name = currEvent.actorId;

        go.transform.parent = TraceImplement.rootOfActors.transform;//Add it to the root G.O.

        //Add this to the dictionary
        Actors.allActors.Add(currEvent.actorId, go);
        if (VisualizationHandler.sysActorNames.Any(go.transform.name.Contains)) //System actors are different
            go.transform.position = new Vector3(Random.Range(3.5f, 4.5f), 1f, Random.Range(-2.5f, -3.5f)); //A separate area->Marked in the inspector
        else
        {
            go.transform.position = new Vector3(Random.Range(0f, 3.5f), Random.Range(1.25f, 1.9f), Random.Range(-1.5f, 1.5f));
            if (logCreateForEvent)
            {
                //Create a Log of it
                Log newLog = new Log(0, "Actor created : " + currEvent.actorId);
                VisualizationHandler.Handle(newLog);
            }
        }
        SuppressActorResponse sar = new SuppressActorResponse(go.transform.name, true);
        SendMessageContext context = new SendMessageContext(go, "SuppressOnOff", sar, SendMessageOptions.RequireReceiver);
        SendMessageHelper.RegisterSendMessage(context);
    }
    public static void Handle(ActorDestroyed currEvent)
    {

        Destroy(Actors.allActors[currEvent.actorId]);
        Actors.allActors.Remove(currEvent.actorId);

        if (logCreateForEvent)
        {
            //Create a Log of it
            Log newLog = new Log(0, "Actor destroyed : " + currEvent.actorId);
            VisualizationHandler.Handle(newLog);
        }
    }
    public static void Handle(MessageSent currEvent)
    {
        GameObject senderGO = Actors.allActors[currEvent.senderId];
        //Use dictionary of actors to do this
        ActorFunctionality af = senderGO.GetComponent<ActorFunctionality>();
        af.GenerateMessageDiscreetly(Actors.allActors[currEvent.receiverId], currEvent.msg);

        if (logCreateForEvent)
        {
            //Create a Log of it
            Log newLog = new Log(0, "Message sent : " + currEvent.senderId + " to " + currEvent.receiverId + ", message : " + currEvent.msg);
            VisualizationHandler.Handle(newLog);
        }

    }
    public static void Handle(MessageReceived currEvent)
    {
        GameObject recGO = Actors.allActors[currEvent.receiverId];
        //Use dictionary of actors to do this
        ActorFunctionality af = recGO.GetComponent<ActorFunctionality>();
        af.ReceiveMessageFromQueueDiscreetly(Actors.allActors[currEvent.senderId]);

        if (logCreateForEvent)
        {
            //Create a Log of it
            Log newLog = new Log(0, "Message received : " + currEvent.receiverId + ", message : " + currEvent.msg);
            VisualizationHandler.Handle(newLog);
        }
    }


    public static void Handle(MessageDropped currEvent)
    {
        //Do some animation to show disappearing message
        //Currently, this is visualized like MessageReceived
        ActorFunctionality af = Actors.allActors[currEvent.receiverId].GetComponent<ActorFunctionality>();
        af.ReceiveMessageFromQueueDiscreetly();

        if (logCreateForEvent)
        {
            //Create a Log of it
            Log newLog = new Log(0, "Message dropped : " + currEvent.receiverId);
            VisualizationHandler.Handle(newLog);
        }
    }
}
