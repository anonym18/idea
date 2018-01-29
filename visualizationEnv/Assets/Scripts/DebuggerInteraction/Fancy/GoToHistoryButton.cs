using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class GoToHistoryButton : MonoBehaviour {

    public int indexOfDispatcher; //index of the dispatcher


    public void HandleClick()
    {

        StepRequest sr = new StepRequest(indexOfDispatcher);
        NetworkInterface.HandleRequest(sr);
    }
}
