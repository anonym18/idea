using UnityEngine;

public class MessageFunctionality : MonoBehaviour
{
    //Set by ActorFunctionality
    public string msg; //The message carried
    public GameObject sender; //who sent me?
    public GameObject recipient; //who receives me?
    public bool isDiscreet = false;

    public int durationOfLineInSteps; //Number of steps after linerenderer is destroyed
    private float deltaChange;
    public bool isActive = false; //Activity state of the message

    public static int bezierPointResolution;
    //Internal usage for curve drawing
    private int bezierRes; //Number of points in the trajectory

    private int arrayCountKeeper = 0; //Where are we?
    private float t = 0.0f;
    private int stepsOnStart;
    private GameObject lineRenderer; //Reference to LineRenderer set in Start()
    private Vector3 recipientOffset;

    public MarkerRepresentation representationHolding; //Set from MarkerFunctionality attached to actor

    void Start()
    {
        lineRenderer = transform.GetChild(0).gameObject; //This is the LineRenderer. There is also an option to getbyname but this one chose for performance
        if(representationHolding != null && representationHolding.index != -1)
        {
            Color tempCol = representationHolding.colourOfMarker;
            lineRenderer.GetComponent<Renderer>().material.color = tempCol;
        }
        else
            lineRenderer.GetComponent<Renderer>().material.color = Color.magenta;
        stepsOnStart = Trace.numOfStepsElapsed;

        //Set message string
        if(msg ==  null)
        {
            Debug.Log("The message had no msg field.");
        }


        deltaChange = 1.0f / durationOfLineInSteps;
        recipientOffset = recipient.GetComponent<ActorFunctionality>().modelOffset;
        bezierRes = bezierPointResolution;

        if(isDiscreet) //If the message is discreet, then we do not visualize the curve
        {
            transform.DetachChildren();
            transform.position = recipient.transform.position - recipientOffset; //Set position to the recipient
            isActive = false; //Set activity to false 
        }
    }


    void Update()
    {
        if (!UserInputHandler.isPaused) //To enable pausing
        {
            if (isActive)
            {
                if (arrayCountKeeper <= bezierRes && t < 1.0f)
                {
                    arrayCountKeeper++;
                    t = arrayCountKeeper * 1.0f / bezierRes;
                    transform.position = GetBezierPoint(sender.transform.position, new Vector3((sender.transform.position.x + recipient.transform.position.x) / 2, 3f, (sender.transform.position.z + recipient.transform.position.z) / 2), recipient.transform.position - recipientOffset, t);
                    //The overflow of arrayCountKeeper is handled by the MessageSphere
                }
                else
                {
                    isActive = false;
                    transform.DetachChildren(); //Detach the line renderer
                }
            }
            //If not active, wait
        }

    }
    public void NewTraceStep() //Because a message is broadcasted by TraceImplement -> Used for step-by-step transparency
    {
        if (lineRenderer != null) //lineRenderer hasn't been destroyed yet
        {
            if (((stepsOnStart + durationOfLineInSteps) <= Trace.numOfStepsElapsed)) //Destroy linerenderer after durationOfLineInSteps
            { Destroy(lineRenderer); }
           
            else
            {
                Color tempCol;
                tempCol = lineRenderer.GetComponent<Renderer>().material.color;
                tempCol.a -= deltaChange; //reduce alpha by a delta amount

                lineRenderer.GetComponent<Renderer>().material.color = tempCol;
            }
            
        }
    }

    Vector3 GetBezierPoint(Vector3 p0, Vector3 p1, Vector3 p2, float t) //Thanks to Wikipedia & catlikecoding
    {
        t = Mathf.Clamp01(t);
        float oneMinusT = 1f - t;
        return
            oneMinusT * oneMinusT * p0 +
            2f * oneMinusT * t * p1 +
            t * t * p2;
    }
    public void ClearMark() //Clear the representation held
    {
        representationHolding = new MarkerRepresentation(); //clear the representation held
    }

    public void DeleteTrail()
    {
        Debug.Log("Deleting trail renderer unnaturally (perhaps because the actor moved)");
        Destroy(lineRenderer);
    }
    void OnDestroy() //Delete the line renderer when the message is destroyed
    {
        Debug.Log("Deleting the trail renderer to " + recipient.name + " as message has been consumed");
        Destroy(lineRenderer);
    }
}
