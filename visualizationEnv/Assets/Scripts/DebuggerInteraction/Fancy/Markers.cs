using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Markers : MonoBehaviour
{
    public static List<MarkerRepresentation> listOfMarkerColoursAvailable;
    public static List<GameObject> markedObjects;
    private void Awake()
    {
        Initialize();
    }


    public static void Initialize()
    {
        listOfMarkerColoursAvailable = new List<MarkerRepresentation>(2);
        listOfMarkerColoursAvailable.Add(new MarkerRepresentation(1, new Color(1f, 0f, 0f, 0.1f)));
        listOfMarkerColoursAvailable.Add(new MarkerRepresentation(2, new Color(0f, 0f, 1f, 0.1f)));

        markedObjects = new List<GameObject>();
    }
    public static MarkerRepresentation AssignNewMarker()
    {
        if (listOfMarkerColoursAvailable.Count > 0)
        {
            MarkerRepresentation retVal = listOfMarkerColoursAvailable[0]; //give it the top-most item
            listOfMarkerColoursAvailable.RemoveAt(0);
            return retVal;
        }
        else
        {
            Debug.LogError("No marker colour left");
            return new MarkerRepresentation(); //The -1 specifies that this is not useful
        }
    }


}

public class MarkerRepresentation
{
    public int index;
    public Color colourOfMarker;

    public MarkerRepresentation(int id, Color col)
    {
        index = id;
        colourOfMarker = col;
    }
    public MarkerRepresentation() //An unmarked representation
    {
        index = -1;
        colourOfMarker = new Color(0f, 0f, 0f, 0f);
    }
}
