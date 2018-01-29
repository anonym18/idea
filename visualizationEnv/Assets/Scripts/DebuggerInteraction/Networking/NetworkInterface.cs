//Contains interfaces called on by the network connection
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public static class NetworkInterface
{
    public static bool requestResponseBalance = true;
    public static void HandleResponseReceived(string jsonResponse)
    {
        requestResponseBalance = true; //Just got a response to our request

        Debug.Log("Debugger sent us " + jsonResponse);
        QueryResponse currResponse = (JsonUtility.FromJson<QueryResponse>(jsonResponse));
        
        switch (currResponse.responseType)
        {
            case "ACTION_RESPONSE":
                //Debug.Log("Received an action response");
                ActionResponse curr = (JsonUtility.FromJson<ActionResponse>(jsonResponse));


                
                //One ActionResponse includes a list of events for one atomic step
                List <ActorEvent> tempList1 = new List<ActorEvent>();
                foreach (string ev in curr.events)
                    tempList1.Add(EventUnwrapper(ev));
                if (tempList1.Count > 0)
                {
                    int stepNum = curr.stepNum;

                    Trace.visualizationToDispatcherIndexMapper.Add(Trace.allEvents.Count, stepNum); //stepNum may be -1 for invalid dispatcher steps

                    Trace.allEvents.Add(tempList1);
                }
                foreach (State st in curr.states)
                    StateUnwrapper(st);

                break;

            case "TOPOGRAPHY_RESPONSE":
                TopographyResponse tr = (JsonUtility.FromJson<TopographyResponse>(jsonResponse));
                //One TopographyResponse includes the type and ordered list of actors, which need to be unwrapped
                TopographyUnwrapper(tr);
                break;

            case "TAG_RESPONSE":
                TagActorResponse tar = (JsonUtility.FromJson<TagActorResponse>(jsonResponse));
                TagResponseUnwrapper(tar);
                break;

            case "TAG_REACHED_RESPONSE":
                TagReachedResponse trr = (JsonUtility.FromJson<TagReachedResponse>(jsonResponse));
                TagReachedResponseUnwrapper(trr);
                break;
            case "EOT_RESPONSE":
                EOTResponseUnwrapper();
                break;
            case "SUPPRESS_ACTOR_RESPONSE":
                SuppressActorResponse sar = JsonUtility.FromJson<SuppressActorResponse>(jsonResponse);
                SuppressResponseUnwrapper(sar);
                break;

            case "STEP_RESPONSE":

                StepResponse sr = (JsonUtility.FromJson<StepResponse>(jsonResponse));

                int id = sr.stepNum; //Store the StepNum  

                Debug.Log("Received response of step " + (id+1));

                List<ActorEvent> listOfEvents = new List<ActorEvent>(); //This list consists of the net difference we need to do

                //Unwrap specific parts in the same way
                foreach (string ev in sr.events)
                    Trace.stepEvents.Add(EventUnwrapper(ev)); //Send these events to a special list

                //State unwrapping is done later as the actors at prior stepNum are not the same as current stepNum
                //StateUnwrapping done in TraceImplement

                //Send message to Reevaluate log- newer logs need to be deleted
                SendMessageContext context = new SendMessageContext(VisualizationHandler.logHead, "ReevaluateList", id + 1, SendMessageOptions.RequireReceiver);
                SendMessageHelper.RegisterSendMessage(context);                                                     //state at n -1 is sent
                break;


            default:
                Debug.LogError("Unable to resolve to a particular class");
                break;
        }

    }

    private static ActorEvent EventUnwrapper(string line)
    {
        ActorEvent ev = (JsonUtility.FromJson<ActorEvent>(line)); //Read into the event parent class
        //Now assign a specific class
        switch (ev.eventType) //Go through all possible event types
        {
            case "ACTOR_CREATED":
                ActorCreated specificActorCreatedEvent = (JsonUtility.FromJson<ActorCreated>(line));
                ev = specificActorCreatedEvent;
                break;
            case "MESSAGE_SENT":
                MessageSent specificMessageSentEvent = (JsonUtility.FromJson<MessageSent>(line));
                ev = specificMessageSentEvent;
                break;
            case "MESSAGE_RECEIVED":
                MessageReceived specificMessageReceivedEvent = (JsonUtility.FromJson<MessageReceived>(line));
                ev = specificMessageReceivedEvent;
                break;
            case "ACTOR_DESTROYED":
                ActorDestroyed specificActorDestroyedEvent = (JsonUtility.FromJson<ActorDestroyed>(line));
                ev = specificActorDestroyedEvent;
                break;
            case "LOG":
                Debug.Log("Log received");
                Log newLog = (JsonUtility.FromJson<Log>(line));
                ev = newLog;
                break;
            case "MESSAGE_DROPPED":
                MessageDropped specificMessageDroppedEvent = (JsonUtility.FromJson<MessageDropped>(line));
                ev = specificMessageDroppedEvent;
                break;
            default:
                //We don't know what this event is
                Debug.LogError("Unknown event encountered in JSON");
                break;
        }
        return ev;
    }
    public static void StateUnwrapper(State st) //Also accessed from TraceImplement
    {
        //Send the state to Actor
        GameObject actorConcerned = Actors.allActors[st.actorId];
        //Make the send message threadsafe
        SendMessageContext context = new SendMessageContext(actorConcerned, "NewStateReceived", st, SendMessageOptions.RequireReceiver);
        SendMessageHelper.RegisterSendMessage(context);
    }


    private static void TopographyUnwrapper(TopographyResponse tr)
    {
        VisualizationHandler.Handle(tr);
    }
    private static void TagResponseUnwrapper(TagActorResponse tar)
    {
        GameObject actorConcerned = Actors.allActors[tar.actorId];
        //Make the send message threadsafe
        SendMessageContext context = new SendMessageContext(actorConcerned, "TagUntag", tar, SendMessageOptions.RequireReceiver);
        SendMessageHelper.RegisterSendMessage(context);
    }
    private static void TagReachedResponseUnwrapper(TagReachedResponse trr)
    {
        AutoNext.ResetEverything(); //Disable Auto-next

        GameObject actorConcerned = Actors.allActors[trr.actorId];
        //Make the send message threadsafe
        SendMessageContext context = new SendMessageContext(actorConcerned, "TagReached", trr, SendMessageOptions.RequireReceiver);
        SendMessageHelper.RegisterSendMessage(context);
    }

    private static void EOTResponseUnwrapper()
    {
        //EOT is reached-> No more Auto-next
        AutoNext.ResetEverything();
        Debug.Log("EOT reached");
    }

    private static void SuppressResponseUnwrapper(SuppressActorResponse sar)
    {
        //Suppress response reached-> inform accordingly
        GameObject actorConcerned = Actors.allActors[sar.actorId];
        //Make the send message threadsafe
        SendMessageContext context = new SendMessageContext(actorConcerned, "SuppressOnOff", sar, SendMessageOptions.RequireReceiver);
        SendMessageHelper.RegisterSendMessage(context);
    }

    public static void HandleTagUntagRequestToBeSent(bool toggle, string actorId)
    {
        if (CheckAndResetRequestPossibility())
        {
            TagActorRequest curr = new TagActorRequest(actorId, toggle);
            string toSend = JsonUtility.ToJson(curr);
            AsynchronousClient.Send(AsynchronousClient.client, toSend);
            Debug.Log("We sent a tag actor request");
        }
    }

    public static void HandleRequest(StateRequest curr)
    {
        if (CheckAndResetRequestPossibility())
        {

            string toSend = JsonUtility.ToJson(curr);
            AsynchronousClient.Send(AsynchronousClient.client, toSend);
        }
    }

    public static void HandleRequest(ActionRequest curr)
    {
        if (CheckAndResetRequestPossibility())
        {
            string toSend = JsonUtility.ToJson(curr);
            AsynchronousClient.Send(AsynchronousClient.client, toSend);
        }
    }
    public static void HandleRequest(TopographyRequest curr)
    {
        if (CheckAndResetRequestPossibility())
        {
            string toSend = JsonUtility.ToJson(curr);
            AsynchronousClient.Send(AsynchronousClient.client, toSend);
        }
    }

    public static void HandleRequest(SuppressActorRequest curr)
    {
        if (CheckAndResetRequestPossibility())
        {
            string toSend = JsonUtility.ToJson(curr);
            AsynchronousClient.Send(AsynchronousClient.client, toSend);
        }
    }

    public static void HandleRequest(StepRequest curr)
    {
        string toSend = JsonUtility.ToJson(curr);
        AsynchronousClient.Send(AsynchronousClient.client, toSend);

        Debug.Log("We sent a request for step " + curr.stepNum);
    }

    public static bool CheckAndResetRequestPossibility()
    {
        if (requestResponseBalance)
        {
            requestResponseBalance = false; //Set to false as something will now be sent
            return true;
        }
        else
            return false;
    }

}

