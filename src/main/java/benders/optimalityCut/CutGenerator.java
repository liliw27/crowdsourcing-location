package benders.optimalityCut;

import benders.master.MipData;
import ilog.concert.IloException;
import ilog.concert.IloRange;
import model.Instance;

import java.util.List;

/**
 * Created by liwang on 11/17/2022.
 */
public abstract class CutGenerator {

//    protected final Logger logger = LoggerFactory.getLogger(CutGenerator.class);

    protected final Instance instance;
    protected final MipData mipData;

    public CutGenerator(Instance instance, MipData mipData){
        this.instance=instance;
        this.mipData=mipData;
    }
    public abstract List<IloRange> generateInqualities() throws IloException;
}
