using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class MessageQueueFunctionality : MonoBehaviour
{
    //3D text and other prefabs that actor nodes own the asses of
    private static GameObject prefabNameText; //A reference to the prefab that is used for each individual actor
    private GameObject contentText; //Reference to the varScreen

    //The queue of current messages
    public Queue<GameObject> messageQueue = new Queue<GameObject>(); //Init queue

    private Material mat; //Holds the material
    public Vector3 originalPosition; //Used to revert to original position after the actor has snapped into focus area once

    void Awake()
    {
        if (!prefabNameText)
            prefabNameText = Resources.Load("NameText") as GameObject;

        mat = GetComponent<Material>();
        mat = new Material(Resources.Load("White") as Material);

        GetComponent<Renderer>().enabled = false;
    }
    
    // Use this for initialization
    void Start()
    {
        if (prefabNameText != null)
        {
            contentText = Instantiate(prefabNameText);
            contentText.transform.parent = this.transform;
            contentText.transform.position = this.transform.position + new Vector3 (0f, 0.13f, 0f); //With a small offset
            contentText.SetActive(false); //Initially not visible
            contentText.GetComponent<TextMesh>().text = "";
        }
        else
            Debug.LogError("Error! Prefab not found!");

        //Set material
        this.GetComponent<MeshRenderer>().material = mat;

        originalPosition = transform.position; //Set the original position
    }

    //---------------------------------------------------------------------------------------------------------------------------------------------------
    //Auxiliary functions

    public void EnqueueToMsgQueue(GameObject msgObj)
    {
        messageQueue.Enqueue(msgObj);
        GetComponent<Renderer>().enabled = true;

        // update the message queue info
        string headMsgText = messageQueue.Peek().GetComponent<MessageFunctionality>().msg;
        contentText.GetComponent<TextMesh>().text = "Number of messages: " + messageQueue.Count + " \nNext Message: " + headMsgText;
    }

    public GameObject DequeueFromMsgQueue()
    {
        GameObject msg = messageQueue.Dequeue();
       
        if (messageQueue.Count == 0)
        {
            // do not show message queue object
            GetComponent<Renderer>().enabled = false;
            // (deactivating contentText when there are no messages disrupts the synchronization of the windows for the actor and its message queue)
            contentText.GetComponent<TextMesh>().text = "";
        }
        else
        {
            string headMsgText = messageQueue.Peek().GetComponent<MessageFunctionality>().msg;
            contentText.GetComponent<TextMesh>().text = "Number of messages: " + messageQueue.Count + " \nNext Message: " + headMsgText;
        }

        return msg;
    }

    public void EmptyQueue() //Called from TraceImplement to clear the entire queue while restructructing actors for consistent history
    {
        while (messageQueue.Count > 0)
        {
            GameObject go = messageQueue.Dequeue();
            Destroy(go);
        }
        GetComponent<Renderer>().enabled = false;
        contentText.GetComponent<TextMesh>().text = "";
    }

    public void ToggleMsgQueueInfo()
    {
        if(contentText.activeSelf)
        {
            contentText.SetActive(false);
        }
        else
        {

            contentText.SetActive(true);
        }

    }

}



