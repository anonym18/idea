using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class SingularLogFuctionality : MonoBehaviour {
    public GameObject prefabHostoryButton; //Set in the inspector
    private GameObject historyInstance;
    public Vector3 positionalOffset = new Vector3(-1.474f,-0.803f,0f);

    public int index;
	// Use this for initialization
	void Start () {


	}
	
	// Update is called once per frame
	void Update () {
		
	}

    public void CreateHistoryButtonIfValid (int id)
    {
        if (id > -1)
        {
            Debug.Log("Creating a new history button as we are at a new valid step");
            historyInstance = Instantiate(prefabHostoryButton);
            historyInstance.transform.parent = transform;
            historyInstance.transform.localPosition = transform.localPosition + positionalOffset;
            historyInstance.GetComponent<GoToHistoryButton>().indexOfDispatcher = id;
        }
    }
}
