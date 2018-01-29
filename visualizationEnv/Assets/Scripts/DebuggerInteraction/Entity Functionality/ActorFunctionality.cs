using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class ActorFunctionality : MonoBehaviour
{
    //3D text and other prefabs that actor nodes own the asses of
    public GameObject prefabNameText; //A reference to the prefab that is used for each individual actor
    private GameObject nameText; 
    
    public GameObject prefabMessageSphereInstance;
    public GameObject prefabMessageQueueBox;


    private VRTK.Highlighters.VRTK_OutlineObjectCopyHighlighter outliner;

    public bool suppressed = false; //Initially, not suppressed

    public bool getState = false; //Is the state shown (or not)?


    public Vector3 originalPosition; //Used to revert to original position after the actor has snapped into focus area once
    public Vector3 modelOffset; //Because models aren't always oriented at 0,0,0
    //Message queue
    public GameObject messageQueueBox;

    //States
    public GameObject prefabVarScreen;
    private GameObject varScreen; //Reference to the varScreen
    private Material mat;
    private Color initialColour;
    void Awake()
    {
        //Set up stuff for Outlining
        outliner = gameObject.GetComponent<VRTK.Highlighters.VRTK_OutlineObjectCopyHighlighter>();
        outliner.Initialise(); //Initialize with an outline colour

    }

    // Use this for initialization
    void Start()
    {
        Debug.Log("Created actor called " + transform.name);
        //My name is..
        if (prefabNameText != null)
        {
            nameText = Instantiate(prefabNameText);
            nameText.GetComponent<TextMesh>().text = this.gameObject.name; //say my name..
            nameText.transform.parent = this.transform; //Who's the daddy?
            nameText.transform.position = this.transform.position; //With no offset
        }
        else
            Debug.LogError("Error! Prefab not found!");

        varScreen = Instantiate(prefabVarScreen);
        varScreen.transform.parent = this.transform; //Who's the daddy?
        varScreen.transform.position = this.transform.position - new Vector3(0, 0.3f, 0); //With a small offset
        varScreen.SetActive(false); //Initially not visible


        originalPosition = transform.position; //Set the original position

        messageQueueBox = Instantiate(prefabMessageQueueBox);
        messageQueueBox.transform.parent = transform; //Who's the daddy?
        messageQueueBox.transform.position = transform.position + new Vector3(0f, 0.1f, 0f) - modelOffset; //With no offset


        mat = GetComponent<MeshRenderer>().material; //Set the value of mat to material attached to renderer
        initialColour = mat.color; //Set initial colour so as to enable reset
    }

    //---------------------------------------------------------------------------------------------------------------------------------------------------
    //Auxiliary functions


    //Outlining
    public void MomentaryOutline(Color outlineC, float t)
    {
        outliner.Highlight(outlineC);
        StartCoroutine(RemoveOutlineAfterTime(t));
    }

    public IEnumerator RemoveOutlineAfterTime(float t)
    {
        yield return new WaitForSeconds(t);
        outliner.Unhighlight();
    }


    //Messaging
    public void GenerateMessage(GameObject recipient, string text)
    {
        GameObject MessageSphereInstance = Instantiate(prefabMessageSphereInstance, transform.position, transform.rotation) as GameObject; //Instantiate message sphere prefab
        MessageSphereInstance.transform.parent = recipient.transform;
        
        MessageFunctionality mf = MessageSphereInstance.GetComponent<MessageFunctionality>();
        mf.sender = this.gameObject; //Tell the message who the sender is
        mf.recipient = recipient;
        mf.msg = text;
        
        //For spreading marking appropriately
        mf.representationHolding = GetComponent<MarkerFunctionality>().representationHolding; //Set the current marker to the message

        
        Debug.Log("New instance of message created from " + this.gameObject.ToString() + " for " + recipient.gameObject.ToString());
        mf.isActive = true;

        ActorFunctionality raf = recipient.GetComponent<ActorFunctionality>();
        if (raf.messageQueueBox.GetComponent<MessageQueueFunctionality>().messageQueue.Count == 0)
        {
            //set the status accordingly (this is the only message in the queue)
            recipient.GetComponent<MarkerFunctionality>().MessageMark(mf.representationHolding);
        }
        raf.messageQueueBox.GetComponent<MessageQueueFunctionality>().EnqueueToMsgQueue(MessageSphereInstance); //Enqueue the message
    }

    public void GenerateMessageDiscreetly(GameObject recipient, string text)
    {
        GameObject MessageSphereInstance = Instantiate(prefabMessageSphereInstance, transform.position, transform.rotation) as GameObject; //Instantiate message sphere prefab
        MessageSphereInstance.transform.parent = recipient.transform;

        MessageFunctionality mf = MessageSphereInstance.GetComponent<MessageFunctionality>();
        mf.sender = gameObject; //Tell the message who the sender is
        mf.recipient = recipient;
        mf.msg = text;
        mf.isDiscreet = true;

        //For spreading marking appropriately
        mf.representationHolding = GetComponent<MarkerFunctionality>().representationHolding; //Set the current marker to the message


        Debug.Log("New instance of message created from " + this.gameObject.ToString() + " for " + recipient.gameObject.ToString());
        mf.isActive = true;

        ActorFunctionality raf = recipient.GetComponent<ActorFunctionality>();
        if (raf.messageQueueBox.GetComponent<MessageQueueFunctionality>().messageQueue.Count == 0)
        {
            //set the status accordingly (this is the only message in the queue)
            recipient.GetComponent<MarkerFunctionality>().MessageMark(mf.representationHolding);
        }
        raf.messageQueueBox.GetComponent<MessageQueueFunctionality>().EnqueueToMsgQueue(MessageSphereInstance); //Enqueue the message
    }

    public void ReceiveMessageFromQueue() //Used for MessageDroppped
    {
        GameObject consumedMessage = messageQueueBox.GetComponent<MessageQueueFunctionality>().DequeueFromMsgQueue(); //Consume message from queue
        Debug.Log("Message " + consumedMessage.ToString() + " dropped by " + this.gameObject.ToString());
        Destroy(consumedMessage);
    }


    public void ReceiveMessageFromQueue(GameObject sender)
    {
        MarkerFunctionality mrkrf = GetComponent<MarkerFunctionality>();
        MessageQueueFunctionality mqf = messageQueueBox.GetComponent<MessageQueueFunctionality>();

        GameObject consumedMessage = messageQueueBox.GetComponent<MessageQueueFunctionality>().DequeueFromMsgQueue(); //Consume message from queue

        //check queue status and do marking logic
        if (mrkrf.status == 1)//Message marked
        {
            mrkrf.Escalate();
        }
        else if (mrkrf.status == -1) //Nothing marked thus far
        {
            if(mqf.messageQueue.Count > 0)
            {
                if(mqf.messageQueue.Peek().GetComponent<MessageFunctionality>().representationHolding.index != -1)
                {
                    //marked
                    mrkrf.MessageMark(mqf.messageQueue.Peek().GetComponent<MessageFunctionality>().representationHolding);
                }
            }
        }
       
        Debug.Log("Message " + consumedMessage.ToString() + " accepted by " + this.gameObject.ToString());
        Destroy(consumedMessage);
    }

    public void ReceiveMessageFromQueueDiscreetly() //Used for MessageDroppped
    {
        GameObject consumedMessage = messageQueueBox.GetComponent<MessageQueueFunctionality>().DequeueFromMsgQueue(); //Consume message from queue
        Debug.Log("Message " + consumedMessage.ToString() + " dropped by " + this.gameObject.ToString());
        Destroy(consumedMessage);
    }


    public void ReceiveMessageFromQueueDiscreetly(GameObject sender)
    {
        MarkerFunctionality mrkrf = GetComponent<MarkerFunctionality>();
        MessageQueueFunctionality mqf = messageQueueBox.GetComponent<MessageQueueFunctionality>();

        GameObject consumedMessage = messageQueueBox.GetComponent<MessageQueueFunctionality>().DequeueFromMsgQueue(); //Consume message from queue

        //check queue status and do marking logic
        if (mrkrf.status == 1)//Message marked
        {
            mrkrf.Escalate();
        }
        else if (mrkrf.status == -1) //Nothing marked thus far
        {
            if (mqf.messageQueue.Count > 0)
            {
                if (mqf.messageQueue.Peek().GetComponent<MessageFunctionality>().representationHolding.index != -1)
                {
                    //marked
                    mrkrf.MessageMark(mqf.messageQueue.Peek().GetComponent<MessageFunctionality>().representationHolding);
                }
            }
        }

        Debug.Log("Message " + consumedMessage.ToString() + " accepted by " + this.gameObject.ToString());
        Destroy(consumedMessage);
    }

    public void MoveToAPosition(Vector3 pos)
    {
        transform.position = pos;
        originalPosition = transform.position;

        //Delete the trails attached to it
        BroadcastMessage("DeleteTrail");
    }

    //Coloring- state changes
    public void ChangeColour(Color colour)
    {
        
        Debug.Log("About to change colour");
        mat.color = colour;
    }

    public void UpdateState(State st)
    {
        ChangeColour(st.behavior); //change colour of the actor
    }

    public void NewStateReceived(State st) //Broadcast a message about change of state
    {
        if (getState)
        {
            gameObject.BroadcastMessage("UpdateState", st);
            Debug.Log("Updating state on " + transform.name);
        }
        else
            Debug.LogError("Updated state received even though the getState bool is false");
    }


    //-----------------------Variable switches
    public bool ToggleState()
    {
        if (getState)
        {
            getState = false;
            varScreen.SetActive(false); //Disable the var screen
            ChangeColour(initialColour); //Set colour to white- generic
            return false;
        }
        else
        {
            getState = true;
            varScreen.SetActive(true);
            return true;
        }
    }

}



