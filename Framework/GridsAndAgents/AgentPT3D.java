package Framework.GridsAndAgents;

import Framework.Interfaces.AgentToBool;

import java.util.ArrayList;

import static Framework.Util.*;

/**
 * extend the AgentPT3D class if you want agents that exist on a 3D continuous lattice
 * with the possibility of stacking multiple agents on the same typeGrid square
 * @param <T> the extended AgentGrid3D class that the agents will live in
 * Created by rafael on 11/18/16.
 */
public class AgentPT3D<T extends AgentGrid3D> extends Agent3DBase<T>{
    private double xPos;
    private double yPos;
    private double zPos;
    AgentPT3D nextSq;
    AgentPT3D prevSq;

    @Override
    public void MoveSQ(int iNext) {
        if(!alive){
            throw new RuntimeException("attempting to move dead agent");
        }
        RemSQ();
        xPos=G.ItoX(iNext)+0.5;
        yPos=G.ItoY(iNext)+0.5;
        zPos=G.ItoZ(iNext)+0.5;
        iSq=iNext;
        AddSQ(iNext);
    }

    @Override
    void Setup(double i) {
        Setup((int)i);
    }

    @Override
    void Setup(double x, double y) {
        throw new IllegalStateException("shouldn't be adding 3D agent to 2D typeGrid");
    }

    void Setup(double xPos, double yPos, double zPos){
        this.xPos=xPos;
        this.yPos=yPos;
        this.zPos=zPos;
        iSq=G.I(xPos,yPos,zPos);
        AddSQ(iSq);
    }

    @Override
    void Setup(int i) {
        this.xPos=G.ItoX(i)+0.5;
        this.yPos=G.ItoY(i)+0.5;
        this.zPos=G.ItoZ(i)+0.5;
        iSq=G.I(xPos,yPos,zPos);
        AddSQ(iSq);
    }

    @Override
    void Setup(int x, int y) {
        throw new IllegalStateException("shouldn't be adding 3D agent to 2D typeGrid");
    }

    void Setup(int xPos,int yPos,int zPos){
        this.xPos=xPos+0.5;
        this.yPos=yPos+0.5;
        this.zPos=zPos+0.5;
        iSq=G.I(xPos,yPos,zPos);
        AddSQ(iSq);
    }
    void AddSQ(int i){
        if(G.grid[i]!=null){
            ((AgentPT3D)G.grid[i]).prevSq=this;
            this.nextSq=(AgentPT3D)G.grid[i];
        }
        G.grid[i]=this;
        G.counts[i]++;
    }
    @Override
    int GetCountOnSquareEval(AgentToBool evalAgent) {
        int ct=0;
        AgentPT3D curr=this;
        while (curr!=null){
            if(evalAgent.EvalAgent(curr)){
                ct++;
                curr=curr.nextSq;
            }
        }
        return ct;
    }
    void RemSQ(){
        if(G.grid[iSq]==this){
            G.grid[iSq]=this.nextSq;
        }
        if(nextSq!=null){
            nextSq.prevSq=prevSq;
        }
        if(prevSq!=null){
            prevSq.nextSq=nextSq;
        }
        prevSq=null;
        nextSq=null;
        G.counts[iSq]--;
    }
    /**
     * Moves the agent to the specified coordinates
     */
    public void MoveSQ(int newX, int newY, int newZ){
        if(!alive){
            throw new RuntimeException("Attempting to move dead agent!");
        }
        int oldX=(int)xPos;
        int oldY=(int)yPos;
        int oldZ=(int)zPos;
        RemSQ();
        iSq=G.I(newX,newY,newZ);
        AddSQ(iSq);
        this.xPos=newX+0.5;
        this.yPos=newY+0.5;
        this.zPos=newZ+0.5;
    }

    /**
     * Moves the agent to the specified coordinates
     */
    public void MovePT(double newX, double newY, double newZ){
        int xIntNew=(int)newX;
        int yIntNew=(int)newY;
        int zIntNew=(int)newZ;
        int xIntOld=(int)xPos;
        int yIntOld=(int)yPos;
        int zIntOld=(int)zPos;
        if(!alive){
            throw new RuntimeException("Attempting to move dead agent!");
        }
        if(xIntNew!=xIntOld||yIntNew!=yIntOld||zIntNew!=zIntOld) {
            RemSQ();
            iSq=G.I(xIntNew,yIntNew,zIntNew);
            AddSQ(iSq);
        }
        xPos=newX;
        yPos=newY;
        zPos=newZ;
    }


    public void MoveSafePT(double newX, double newY, double newZ) {
        if(!alive){
            throw new RuntimeException("Attempting to move dead agent!");
        }
        if (G.In(newX, newY, newZ)) {
            MovePT(newX, newY, newZ);
            return;
        }
        if (G.wrapX) {
            newX = ModWrap(newX, G.moveSafeXdim);
        } else if (!InDim(newX, G.xDim)) {
            newX = Xpt();
        }
        if (G.wrapY) {
            newY = ModWrap(newY, G.moveSafeYdim);
        } else if (!InDim(newY, G.yDim)) {
            newY = Ypt();
        }
        if (G.wrapZ) {
            newZ = ModWrap(newZ, G.moveSafeZdim);
        } else if (!InDim(newZ, G.zDim)) {
            newZ = Zpt();
        }
        MovePT(newX,newY,newZ);
    }
    /**
     * gets the xDim coordinate of the agent
     */
    public double Xpt(){
        return xPos;
    }

    /**
     * gets the yDim coordinate of the agent
     */
    public double Ypt(){
        return yPos;
    }

    /**
     * gets the z coordinate of the agent
     */
    public double Zpt(){
        return zPos;
    }

    /**
     * gets the xDim coordinate of the square that the agent occupies
     */
    public int Xsq(){
        return (int)xPos;
    }

    /**
     * gets the yDim coordinate of the square that the agent occupies
     */
    public int Ysq(){
        return (int)yPos;
    }

    /**
     * gets the z coordinate of the square that the agent occupies
     */
    public int Zsq(){ return (int)zPos; }

    public void Dispose(){
        if(!alive){
            throw new RuntimeException("attepting to dispose already dead agent");
        }
        RemSQ();
        G.agents.RemoveAgent(this);
        if(myNodes!=null){
            myNodes.DisposeAll();
        }
    }

    @Override
    void GetAllOnSquare(ArrayList<AgentBaseSpatial> putHere) {
        AgentPT3D toList=this;
        while (toList!=null){
            putHere.add(toList);
            toList=toList.nextSq;
        }
    }

    @Override
    void GetAllOnSquareEval(ArrayList<AgentBaseSpatial> putHere, AgentToBool evalAgent) {
        AgentPT3D toList=this;
        while (toList!=null){
            if(evalAgent.EvalAgent(toList)) {
                putHere.add(toList);
            }
            toList=toList.nextSq;
        }
    }

    @Override
    int GetCountOnSquare() {
        return G.counts[Isq()];
    }
    public int GetAge(){
        return G.GetTick()-birthTick;
    }

    public<T extends AgentPT3D> double Xdisp(T other, boolean wrapX){
        return wrapX? DispWrap(other.Xpt(),Xpt(), G.xDim):Xpt()-other.Xpt();
    }
    public<T extends AgentPT3D> double Xdisp(T other){
        return G.wrapX? DispWrap(other.Xpt(),Xpt(), G.xDim):Xpt()-other.Xpt();
    }
    public <T extends AgentPT3D> double Ydisp(T other, boolean wrapY){
        return wrapY? DispWrap(other.Ypt(),Ypt(), G.yDim):Ypt()-other.Ypt();
    }
    public <T extends AgentPT3D> double Ydisp(T other){
        return G.wrapY? DispWrap(other.Ypt(),Ypt(), G.yDim):Ypt()-other.Ypt();
    }
    public <T extends AgentPT3D> double Zdisp(T other, boolean wrapZ){
        return wrapZ? DispWrap(other.Zpt(),Zpt(), G.zDim):Zpt()-other.Zpt();
    }
    public <T extends AgentPT3D> double Zdisp(T other){
        return G.wrapZ? DispWrap(other.Zpt(),Zpt(), G.zDim):Zpt()-other.Zpt();
    }

    public <T extends AgentPT3D> double disp(T other, boolean wrap){
        double dx = Xdisp(other, wrap);
        double dy = Ydisp(other, wrap);
        double dz = Zdisp(other, wrap);
        return Norm(dx, dy, dz);
    }
    public <T extends AgentPT3D> double disp(T other){
        double dx = Xdisp(other, G.wrapX);
        double dy = Ydisp(other, G.wrapY);
        double dz = Zdisp(other, G.wrapZ);
        return Norm(dx, dy, dz);
    }
}
