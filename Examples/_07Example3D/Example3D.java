package Examples._07Example3D;
import Framework.GridsAndAgents.AgentGrid3D;
import Framework.GridsAndAgents.AgentSQ3D;
import Framework.GridsAndAgents.Grid2Ddouble;
import Framework.GridsAndAgents.PDEGrid3D;
import Framework.Gui.GridVisWindow;
import Framework.Gui.GuiGridVis;
import Framework.Gui.Vis3DOpenGL;
import java.util.LinkedList;
import java.util.Random;

import static Examples._07Example3D.Example3D.*;
import static Framework.Utils.*;

class ExCell3D extends AgentSQ3D<Example3D>{
    int type;
    void InitVessel(){
        type= VESSEL;
        G().vessels.add(this);
    }
    void InitTumor(){
        type= TUMOR;
    }
    boolean Metastasis(){
        double resVal=G().resource.Get(Isq());
        if(resVal>G().METASTASIS_CONC&&G().rn.nextDouble()<G().METASTASIS_PROB){
            return true;
        }
        return false;
    }
    void Metabolism(){
        switch (type){
            case VESSEL: G().resource.Set(Isq(),G().VESSEL_CONC); break;
            case TUMOR: G().resource.Mul(Isq(),G().TUMOR_METABOLISM_RATE); break;
        }
    }
    boolean Death(){
        double resVal=G().resource.Get(Isq());
        return resVal<G().DEATH_CONC && G().rn.nextDouble()<(1.0-resVal/G().DEATH_CONC);
    }
    boolean Divide(){
        return G().rn.nextDouble()<G().resource.Get(Isq());
    }
    void CellStep(){
        if(type== TUMOR){
            G().countTumor++;
            if(Death()){
                Dispose();
                return;
            }
            if(Divide()){
                int nDivOpts=HoodToEmptyIs(G().vnHood,G().divIs);
                if(nDivOpts>1){
                    G().NewAgentSQ(G().divIs[G().rn.nextInt(nDivOpts)]).InitTumor();
                }
            }
            if(Metastasis()){//choose random vessel location to metastasize to
                int whichVessel=G().rn.nextInt(G().vessels.size());
                int nMetOpts=G().vessels.get(whichVessel).HoodToEmptyIs(G().vnHood,G().divIs);
                if(nMetOpts>1){
                    G().NewAgentSQ(G().divIs[G().rn.nextInt(nMetOpts)]).InitTumor();
                }
            }
        }
    }
}
public class Example3D extends AgentGrid3D<ExCell3D> {
    //vessels: cells eat off them, and can go metastatic
    int countTumor;
    final static int BACKGROUND_COLOR =RGB256(0,0,0), VESSEL_COLOR =RGB256(255,78,68);
    final static int VESSEL=0,TUMOR=1;
    double DIFF_RATE=1.0/6;//maximum stable diffusion rate
    double TUMOR_METABOLISM_RATE =0.96;
    double NORMAL_METABOLISM_RATE =0.99;
    double VESSEL_CONC=1;
    double DEATH_CONC=0.01;
    double METASTASIS_PROB=0.00001;
    double METASTASIS_CONC=0.3;
    int[]vnHood=VonNeumannHood3D(false);
    int[]divIs=new int[vnHood.length/2];
    PDEGrid3D resource;
    Random rn=new Random();
    LinkedList<ExCell3D> vessels=new LinkedList<>();//used to make metastasis more efficient (and as an example)
    public Example3D(int x, int y, int z) {
        super(x, y, z, ExCell3D.class);
        resource=new PDEGrid3D(x,y,z);
    }
    public void DiffStep(){
        for (ExCell3D cellOrVessel : this) {
            cellOrVessel.Metabolism();
        }
        resource.MulAll(NORMAL_METABOLISM_RATE);
        resource.Diffusion(DIFF_RATE);
    }
    public void StepAll(){
        countTumor=0;
        for (ExCell3D cell : this) {
            cell.CellStep();
        }
        if(countTumor==0){//ensure that the model is seeded
            int placeLoc=rn.nextInt(length);
            if(GetAgent(placeLoc)==null){
                NewAgentSQ(placeLoc).InitTumor();
            }
        }
        DiffStep();
       IncTick();
    }
    public int GenVessels(double vesselSpacingMin){
        //create a Grid to store the locations that are too close for placing another vessel
        Grid2Ddouble openSpots=new Grid2Ddouble(xDim,zDim);
        //create a neighborhood that defines all indices that are too close
        int[]vesselSpacingHood=CircleHood(false,vesselSpacingMin);
        int[]markIs=new int[vesselSpacingHood.length/2];
        //shuffle an array of all indices, so we search them in random order
        int[]indicesToTry=GenIndicesArray(openSpots.length);
        Shuffle(indicesToTry,rn);
        int vesselCt=0;
        for (int i : indicesToTry) {
            if(openSpots.Get(i)==0){
                int x=openSpots.ItoX(i);
                int y=openSpots.ItoY(i);
                GenVessel(x,y);
                vesselCt++;
                int nSpots=openSpots.HoodToIs(vesselSpacingHood,markIs,x,y,true,true);
                for (int j = 0; j < nSpots; j++) {
                    //mark spot as too close for another vessel
                    openSpots.Set(markIs[j],-1);
                }
            }
        }
        return vesselCt;
    }
    public void DrawCells(Vis3DOpenGL vis){
        vis.Clear(BACKGROUND_COLOR);
        for (ExCell3D cellOrVessel : this) {
            switch (cellOrVessel.type){
                case VESSEL: vis.Circle(cellOrVessel.Xsq(),cellOrVessel.Ysq(),cellOrVessel.Zsq(),1, VESSEL_COLOR);break;
                case TUMOR: vis.Circle(cellOrVessel.Xsq(),cellOrVessel.Ysq(),cellOrVessel.Zsq(),0.3,HeatMapBRG(Math.pow(resource.Get(cellOrVessel.Isq()),0.5)*0.8+0.2));
            }
        }
        vis.Show();
    }
    public void DrawResource(GuiGridVis vis){
        for (int x = 0; x < xDim; x++) {
            for (int z = 0; z < zDim; z++) {
                double resSum=0;
                for (int y = 0; y < yDim; y++) {
                    resSum+=resource.Get(x,y,z);
                }
                vis.SetPix(x,z,HeatMapBRG(Math.pow(resSum*1.0/yDim,0.5)));
            }
        }
    }
    public void GenVessel(int x,int z){
        for (int y = 0; y < yDim; y++) {
            //clear out any agents that are in the path of the vessel
            ExCell3D occupant=GetAgent(x,y,z);
            if(occupant!=null){
                occupant.Dispose();
            }
            NewAgentSQ(x,y,z).InitVessel();
        }
    }
    public void GenCells(int initPopSize){
        int[]indicesToTry=GenIndicesArray(length);
        Shuffle(indicesToTry,rn);
        int nCreated=0;
        for (int i = 0; i < length; i++) {
            //check if position is empty before dropping cell
            if(GetAgent(indicesToTry[i])==null){
                NewAgentSQ(indicesToTry[i]).InitTumor();
                nCreated++;
            }
            if(nCreated==initPopSize){
                break;
            }
        }
    }
    public static void main(String[] args) {
        int x=150,y=150,z=10;
        Example3D ex=new Example3D(x,z,y);
        ex.GenVessels(20);
        ex.IncTick();//tick is incremented so that initialized vessels appear
        //Diffuse to steady state
        for (int i = 0; i < 100; i++) {
            ex.DiffStep();
        }
        GridVisWindow visResource=new GridVisWindow(x,y,5);
        Vis3DOpenGL vis=new Vis3DOpenGL(1000,1000,x,y,z,"TumorVis");
        while (!vis.CheckClosed()){
            ex.StepAll();
            ex.DrawCells(vis);
            ex.DrawResource(visResource);
            ex.CleanShuffInc(ex.rn);//Equivalent to calling CleanAgents, ShuffleAgents, and IncTick grid functions
        }
        vis.Dispose();
        visResource.Dispose();
    }
}