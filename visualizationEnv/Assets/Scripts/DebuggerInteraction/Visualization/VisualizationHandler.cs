//Has the 'Log' shared resource

//TODO- Get Actor descriptor as prefab from Resources
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

public class VisualizationHandler : MonoBehaviour
{
   
    public static bool logCreateForEvent = true;
    public static float outlineTime = 1f;

    public static string [] sysActorNames= { "deadLetters", "Timer" };

    public static Dictionary<string, GameObject> modelDictionary;
    public static GameObject logHead; //Set on Awake(), this is the parent of all logs
    public void Awake()
    {

        //TODO: Optimize by removing Resource calls
        modelDictionary = new Dictionary<string, GameObject>();
        GameObject temp; 
        temp = Resources.Load("Cube") as GameObject;
        modelDictionary.Add("Cube", temp);

        temp = Resources.Load("Sphere") as GameObject;
        modelDictionary.Add("Sphere", temp);

        temp = Resources.Load("Capsule") as GameObject;
        modelDictionary.Add("Capsule", temp);

        temp = Resources.Load("Cylinder") as GameObject;
        modelDictionary.Add("Cylinder", temp);

        temp = Resources.Load("Chopstick") as GameObject;
        modelDictionary.Add("Chopstick", temp);

        temp = Resources.Load("Postkasten") as GameObject;
        modelDictionary.Add("Postkasten", temp);

        temp = Resources.Load("Robot") as GameObject;
        modelDictionary.Add("Robot", temp);

        temp = Resources.Load("Smiley") as GameObject;
        modelDictionary.Add("Smiley", temp);

        logHead = GameObject.Find("Logs");
    }
    public static void Handle (ActorCreated currEvent)
    {
        GameObject go;
        if (currEvent.resourceId == "" || currEvent.resourceId == null)
            go = Instantiate(modelDictionary["Cube"]); //If type is not set, we want a cube
        else
            go = Instantiate(modelDictionary[currEvent.resourceId]);

        go.transform.name = currEvent.actorId;

        go.transform.parent = TraceImplement.rootOfActors.transform;//Add it to the root G.O.

        //Add this to the dictionary
        Actors.allActors.Add(currEvent.actorId, go);
        if (sysActorNames.Any(go.transform.name.Contains))
            go.transform.position = new Vector3(Random.Range(3.5f, 4.5f), 1f, Random.Range(-2.5f, -3.5f)); //A separate area->Marked in the inspector
        else
        {
            go.transform.position = new Vector3(Random.Range(0f, 3.5f), Random.Range(1.25f, 1.9f), Random.Range(-1.5f, 1.5f));
            if (logCreateForEvent)
            {
                //Create a Log of it
                Log newLog = new Log(0, "Actor created : " + currEvent.actorId);
                Handle(newLog);
            }
        }
        Actors.allActors[currEvent.actorId].GetComponent<ActorFunctionality>().MomentaryOutline(Color.magenta, outlineTime/2);
    }
    public static void Handle (ActorDestroyed currEvent)
    {
       
        GameObject.Destroy(Actors.allActors[currEvent.actorId]);
        Actors.allActors.Remove(currEvent.actorId);

        if (logCreateForEvent)
        {
            //Create a Log of it
            Log newLog = new Log(0, "Actor destroyed : " + currEvent.actorId);
            Handle(newLog);
        }
    }
    public static void Handle(MessageSent currEvent)
    {
        GameObject senderGO = Actors.allActors[currEvent.senderId];
        //Use dictionary of actors to do this
        ActorFunctionality af = senderGO.GetComponent<ActorFunctionality>();
        af.GenerateMessage(Actors.allActors[currEvent.receiverId], currEvent.msg);

        if (logCreateForEvent)
        {
            //Create a Log of it
            Log newLog = new Log(0, "Message sent : " + currEvent.senderId + " to " + currEvent.receiverId + ", message : " + currEvent.msg);
            Handle(newLog);
        }

    }
    public static void Handle(MessageReceived currEvent)
    {
        GameObject recGO = Actors.allActors[currEvent.receiverId];
        //Use dictionary of actors to do this
        ActorFunctionality af = recGO.GetComponent<ActorFunctionality>(); 
        af.ReceiveMessageFromQueue(Actors.allActors[currEvent.senderId]);

        if (logCreateForEvent)
        {
            //Create a Log of it
            Log newLog = new Log(0, "Message received : " + currEvent.receiverId + ", message : " + currEvent.msg);
            Handle(newLog);
        }
    }
    
    public static void Handle(Log currEvent)
    {
        //Maybe also play an error sound?
        //Send message to the main screen to change the text

        SendMessageContext context = new SendMessageContext(logHead, "NewLog", currEvent, SendMessageOptions.RequireReceiver);
        SendMessageHelper.RegisterSendMessage(context);
     }

    public static void Handle(MessageDropped currEvent)
    {
        //Do some animation to show disappearing message
        //Currently, this is visualized like MessageReceived
        ActorFunctionality af = Actors.allActors[currEvent.receiverId].GetComponent<ActorFunctionality>();
        af.ReceiveMessageFromQueue();

        if (logCreateForEvent)
        {
            //Create a Log of it
            Log newLog = new Log(0, "Message dropped : " + currEvent.receiverId);
            Handle(newLog);
        }
    }

    public static void Handle(TopographyResponse tr)
    {


        switch (tr.topographyType)
        {
            case "RING":
                List<Vector3> positions = RingGenerator.Create(tr.orderedActorIds.Count);

                for (int i = 0; i < tr.orderedActorIds.Count; i++)
                {
                    //This hack because of some random GetComponentFastPath error  
                    GameObject actorConcerned = Actors.allActors[tr.orderedActorIds[i]];
                    SendMessageContext context = new SendMessageContext(actorConcerned, "MoveToAPosition", positions[i], SendMessageOptions.RequireReceiver);
                    SendMessageHelper.RegisterSendMessage(context);
                }
                break;
            default:
                Debug.LogError("Unknown Topography type response received. Doing nothing.");
                break;
        }
    }


    //-------------------Outlining functions
    public static void Outline(ActorCreated currEvent)
    {
        //Currently called from the Handler function
    }
    public static void Outline(ActorDestroyed currEvent)
    {
        Color col = Color.magenta;
        MarkerFunctionality mf = Actors.allActors[currEvent.actorId].GetComponent<MarkerFunctionality>();
        if(mf.status == 2) //Actor is marked
        {
            col = mf.representationHolding.colourOfMarker;
        }
        Actors.allActors[currEvent.actorId].GetComponent<ActorFunctionality>().MomentaryOutline(col, outlineTime);

    }
    public static void Outline(MessageSent currEvent)
    {
        Color col = Color.magenta;
        MarkerFunctionality mf = Actors.allActors[currEvent.senderId].GetComponent<MarkerFunctionality>();
        if (mf.status == 2) //Actor is marked
        {
            col = mf.representationHolding.colourOfMarker;
        }
        Actors.allActors[currEvent.receiverId].GetComponent<ActorFunctionality>().MomentaryOutline(col, outlineTime);
        Actors.allActors[currEvent.senderId].GetComponent<ActorFunctionality>().MomentaryOutline(Color.white, outlineTime);
    }
    public static void Outline(MessageReceived currEvent)
    {
        Color col = Color.magenta;
        MarkerFunctionality mf = Actors.allActors[currEvent.receiverId].GetComponent<MarkerFunctionality>();
        if (mf.status == 2) //Actor is marked
        {
            col = mf.representationHolding.colourOfMarker;
        }
        Actors.allActors[currEvent.senderId].GetComponent<ActorFunctionality>().MomentaryOutline(Color.white, outlineTime);
        Actors.allActors[currEvent.receiverId].GetComponent<ActorFunctionality>().MomentaryOutline(col, outlineTime);
    }

    public static void Outline(Log currEvent)
    {
        //Undefined what really needs to be done
    }

    public static void Outline(MessageDropped currEvent)
    {
        Color col = Color.magenta;
        MarkerFunctionality mf = Actors.allActors[currEvent.receiverId].GetComponent<MarkerFunctionality>();
        if (mf.status == 2) //Actor is marked
        {
            col = mf.representationHolding.colourOfMarker;
        }
        Actors.allActors[currEvent.receiverId].GetComponent<ActorFunctionality>().MomentaryOutline(col, outlineTime);
        Actors.allActors[currEvent.senderId].GetComponent<ActorFunctionality>().MomentaryOutline(Color.white, outlineTime);
    }

}
