using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Suppression : MonoBehaviour
{
    public bool isSuprressed = false;

    public GameObject prefabSuppressionIndicator; //Prefab-> set in the inspector

    private GameObject suppressionIndicator;

    public void SuppressOnOff (SuppressActorResponse sar)
    {
        if (sar.toSuppress)
        {
            isSuprressed = true;
            //Place an indicator to show it
            suppressionIndicator = Instantiate(prefabSuppressionIndicator);
            suppressionIndicator.transform.parent = transform; //Who's the daddy?
            suppressionIndicator.transform.position = transform.position + new Vector3( 0f, 0.5f, 0f); 
        }
        else
        {
            isSuprressed = false;
            Destroy(suppressionIndicator); //Delete indicator to show it
        }
    }
}
