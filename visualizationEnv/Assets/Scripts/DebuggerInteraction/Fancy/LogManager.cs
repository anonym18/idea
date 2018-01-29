//Attached to LogText and uses shared structures VisualizationHandler.logInfo

using System.Collections.Generic;
using UnityEngine;

public class LogManager : MonoBehaviour
{
    static string prevText = "--No recent log--";
    static string colourTagTextEnd = "</color>";
    private static int atomicStepForSingularHistoryExecution = -1;
    private static int currDispatcherStep = -1;

    private List<GameObject> logs; //List of all logs generated

    public GameObject textPrefab;
    public int maxSize = 15; //Max size of log
    public float verticalOffset = 0.22f;
    private void Start()
    {
        logs = new List<GameObject>();
    }


    public void NewLog(Log currLog)
    {

        string colourTagTextStart = "";
        
        switch(currLog.logType)
        {
            case 0: //DEBUG
                colourTagTextStart = "<color=white>";
                break;
            case 1: //INFO
                colourTagTextStart = "<color=yellow>";
                break;
            case 2: //WARNING
                colourTagTextStart = "<color=orange>";
                break;
            case 3: //ERROR
                colourTagTextStart = "<color=red>";
                break;
            default:
                Debug.LogError("Log type not identified");
                colourTagTextStart = "<color=white>";
                break;
        }
        string currText = colourTagTextStart+currLog.text+colourTagTextEnd;
        GameObject newLog = Instantiate(textPrefab);
        newLog.GetComponent<TextMesh>().text = currText;
        newLog.transform.parent = transform;
        newLog.transform.position = transform.position;

        AdjustPositionOfPreviousLogs(); //Adjust position of other logs
        logs.Add(newLog);
        AdjustSize(); //Make sure the log isn't longer than maxSize

        bool valExists = Trace.visualizationToDispatcherIndexMapper.ContainsKey(Trace.pointerToCurrAtomicStep);
        if (valExists)
        {

            int dictValue = -1;
            Trace.visualizationToDispatcherIndexMapper.TryGetValue(Trace.pointerToCurrAtomicStep, out dictValue);

            if (atomicStepForSingularHistoryExecution != Trace.pointerToCurrAtomicStep && dictValue > -1) //Ensures that CreateHistory happens only once
            {
                newLog.GetComponent<SingularLogFuctionality>().CreateHistoryButtonIfValid(dictValue);

                currDispatcherStep = dictValue;
            }

            if (dictValue > -1) //If it was a valid step
                atomicStepForSingularHistoryExecution = Trace.pointerToCurrAtomicStep; //set local index to Atomic step to ensure history button creation happens only once
        }

        newLog.GetComponent<SingularLogFuctionality>().index = currDispatcherStep;

    }
    private void AdjustPositionOfPreviousLogs()
    {
        foreach (GameObject obj in logs)
        {
            obj.transform.position -= new Vector3(0f, verticalOffset, 0f); 
        }
    }
    private void AdjustSize()
    {
        if (logs.Count > maxSize)
        {
            Destroy(logs[0]);
            logs.RemoveAt(0);
        }
    }

    public void ReevaluateList(int stepNum)
    {
        List<GameObject> passableList = new List<GameObject>();
        foreach (GameObject go in logs)
        {
            if (go.GetComponent<SingularLogFuctionality>().index < stepNum)
            {
                passableList.Insert(0,go); //We need this in the beginning
            }
            else
                Destroy(go); //Destroy it
        }
        logs = new List<GameObject>(passableList); //Pass the list we created here to logs
        ReadjustPositions();
    }
    private void ReadjustPositions()
    {
        int counter = 0;
        foreach(GameObject go in logs)
        {
            go.transform.position = transform.position - new Vector3(0f, verticalOffset * counter, 0f);
            counter++;
        }
    }
}
