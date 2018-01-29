using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using VRTK;

public class DetectHistoricalStepClick : MonoBehaviour
{
    // Use this for initialization
    void Start()
    {
        GetComponent<VRTK_ControllerEvents>().TriggerClicked += new ControllerInteractionEventHandler(CheckHistoricalBlock);
    }

    // Update is called once per frame
    void Update()
    {

    }

    private void CheckHistoricalBlock(object sender, ControllerInteractionEventArgs e)
    {
        if (UserInputHandler.laserPointedCollider != null)
        {
            if (UserInputHandler.laserPointedCollider.CompareTag("HistoricalStep"))
            {
                UserInputHandler.laserPointedCollider.GetComponent<GoToHistoryButton>().HandleClick();
            }
        }
    }

}
