/*
 * Short_model.java
 */

import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.File;
import java.util.Scanner;

/** Model exported on Jan 1 2020, 14:44 by COMSOL 5.4.0.388. */
public class circular_model {

// Read parameters	
  public static Model setparams(Model model, String fileName) {
	  File file = new File(fileName);
	  try {
		Scanner sc = new Scanner(file);
	
		while (sc.hasNextLine()){
			String data = sc.nextLine();
			String[] splitData = data.split(" ", 0);
			model.param().set(splitData[0], splitData[1]);
			}
		}
	catch (Exception e) {
		System.out.println("Missing Parameters file :" + fileName);
	}
		return model;
	  
  }
  
// Read vasodilation data from text file  
  
  public static Model readVesselData(Model model, String fileName, String funName) {
	File file = new File(fileName);
	try {
		
		Scanner sc1 = new Scanner(file); // dummy scanner to count lines
		int nLines = 0;
		while (sc1.hasNextLine()){
			nLines++;
			sc1.nextLine();
		}
		sc1.close();
		
		Scanner sc = new Scanner(file);
		String[][] dataLines = new String[nLines][2];
		
		int i = 0;
		while (sc.hasNextLine()){
			String data = sc.nextLine();
			String[] splitData = data.split("\t", 0);
			dataLines[i][0] = splitData[0];
			dataLines[i][1] = splitData[1];
			i++;
			}
		model.func().create(funName, "Interpolation");
		model.func(funName).set("table", dataLines);
		model.func(funName).set("interp", "cubicspline");
		}
	catch (Exception e) {
		System.out.println("Missing data file :" + fileName + e);
	}
		return model;
	  
  }
  
  // read variables from files
  public static Model readVarData(Model model, String fileName, String varName) {
	File file = new File(fileName);
	try {
		Scanner sc = new Scanner(file);
		
		while (sc.hasNextLine()){
			String data = sc.nextLine();
			String[] splitData = data.split(" ", 0);
			model.component("comp1").variable(varName).set(splitData[0], splitData[1]);
			}
		}
	catch (Exception e) {
		System.out.println("Missing variable file :" + fileName + e);
	}
		return model;
  }
   
// Create functions, parameters, geometry, mesh and variables

  public static Model run(String arg) {
    Model model = ModelUtil.create("Model");
	String modelPath = "/gpfs/scratch/rpk5196/Comsol/Brain_3D/sleep/Kevin_vessel/Circular_PVS2";
    model.modelPath(modelPath);

    model.label("circular_model.mph");
	
	String paramsFile = modelPath + "/params.txt";
	model = setparams(model, paramsFile); // set parameters from the paramFile
	
    model.component().create("comp1", true);

   
	// create all the required functions
    model.func().create("step1", "Step");
    model.func().create("step2", "Step");
    model.func().create("an1", "Analytic");
    model.func().create("step3", "Step");
    model.func().create("step4", "Step");
    model.func().create("step5", "Step");
    model.func().create("an2", "Analytic");
    model.func("step1").set("location", 1.2);
    model.func("step1").set("smooth", 1.3);
    model.func("step2").set("location", 3.4);
    model.func("step2").set("smooth", 3.5);
    model.func("an1").set("expr", "step1(x) - step2(x)");
    model.func("an1").set("plotargs", new String[][]{{"x", "0", "5"}});
    model.func("step3").label("Pressure step");
    model.func("step3").set("location", 0.07);
    model.func("step4").label("permeability step");
    model.func("step4").set("location", 225);
    model.func("step4").set("smooth", 10);
    model.func("step5").label("pulsation step");
    model.func("step5").set("location", 0.57);
    String dataFile = modelPath + "/Vessel_filtered_data.txt";
    model = readVesselData(model, dataFile, "int1");
    model.func("an2").set("expr", "0.5*step1(x)*(1-step1(x-73))*(int1(x)-20.0)");
    model.func("an2").set("plotargs", new String[][]{{"x", "0", "80"}});

    // create geometry (set unit)
	model.component("comp1").geom().create("geom1", 3);
    model.component("comp1").geom("geom1").lengthUnit("\u00b5m");
	
	// import mesh
	model.component("comp1").mesh().create("mesh1");
    model.component("comp1").mesh("mesh1").create("imp1", "Import");
    model.component("comp1").mesh("mesh1").feature("imp1").set("source", "native");
    String meshFile = modelPath + "/Full_mesh2.mphtxt";
    model.component("comp1").mesh("mesh1").feature("imp1").set("filename", meshFile);
    model.component("comp1").mesh("mesh1").run();
    
    

	// create variables
    model.component("comp1").variable().create("var1");
    String varFile1 = modelPath + "/Formulation.txt";
    model = readVarData(model, varFile1, "var1");
    
    model.component("comp1").variable().create("var2");
    String varFile2 = modelPath + "/Boundaries.txt";
    model = readVarData(model, varFile2, "var2");
    model.component("comp1").variable("var1").label("Formulation");
    model.component("comp1").variable("var2").label("Boundaries");
    
    // views and componenet couplings
    model.component("comp1").view("view1").set("renderwireframe", true);
    model.component("comp1").cpl().create("intop1", "Integration");
    model.component("comp1").cpl("intop1").selection().geom("geom1", 2);
    model.component("comp1").cpl("intop1").selection().set(5);
    model.component("comp1").cpl("intop1").set("opname", "Outflow");
	
	//solid displacement
    model.component("comp1").physics().create("useq", "WeakFormPDE", "geom1");
    model.component("comp1").physics("useq").identifier("useq");
    model.component("comp1").physics("useq").field("dimensionless").field("us");
    model.component("comp1").physics("useq").field("dimensionless").component(new String[]{"usx", "usy", "usz"});
    model.component("comp1").physics("useq").prop("Units").set("DependentVariableQuantity", "displacement");
    model.component("comp1").physics("useq").selection().set(1);
    model.component("comp1").physics("useq").create("dir1", "DirichletBoundary", 2);
    model.component("comp1").physics("useq").feature("dir1").selection().set(1, 12);
    model.component("comp1").physics("useq").create("dir2", "DirichletBoundary", 2);
    model.component("comp1").physics("useq").feature("dir2").selection().set(16, 17);
    model.component("comp1").physics("useq").create("dir3", "DirichletBoundary", 2);
    model.component("comp1").physics("useq").feature("dir3").selection().set(2, 13);
    model.component("comp1").physics("useq").create("dir4", "DirichletBoundary", 2);
    model.component("comp1").physics("useq").feature("dir4").selection().set(3);
    model.component("comp1").physics("useq").label("Tissue displacement");
    model.component("comp1").physics("useq").feature("wfeq1").set("weak", new String[][]{{"usWC"}, {"0"}, {"0"}});
    model.component("comp1").physics("useq").feature("dir1").set("useDirichletCondition", new int[][]{{1}, {0}, {0}});
    model.component("comp1").physics("useq").feature("dir1").label("Symmetry");
    model.component("comp1").physics("useq").feature("dir2").set("useDirichletCondition", new int[][]{{1}, {0}, {0}});
    model.component("comp1").physics("useq").feature("dir2").label("X edge");
    model.component("comp1").physics("useq").feature("dir3").set("useDirichletCondition", new int[][]{{0}, {1}, {0}});
    model.component("comp1").physics("useq").feature("dir3").label("Y edge");
    model.component("comp1").physics("useq").feature("dir4").set("useDirichletCondition", new int[][]{{0}, {0}, {1}});
    model.component("comp1").physics("useq").feature("dir4").label("Z edge");
    
    
    //solid velocity
    model.component("comp1").physics().create("vseq", "WeakFormPDE", "geom1");
    model.component("comp1").physics("vseq").identifier("vseq");
    model.component("comp1").physics("vseq").field("dimensionless").field("vs");
    model.component("comp1").physics("vseq").field("dimensionless").component(new String[]{"vsx", "vsy", "vsz"});
    model.component("comp1").physics("vseq").prop("Units").set("DependentVariableQuantity", "velocity");
    model.component("comp1").physics("vseq").selection().set(1);
    model.component("comp1").physics("vseq").create("dir1", "DirichletBoundary", 2);
    model.component("comp1").physics("vseq").feature("dir1").selection().set(16);
    model.component("comp1").physics("vseq").create("dir2", "DirichletBoundary", 2);
    model.component("comp1").physics("vseq").feature("dir2").selection().set(2, 13);
    model.component("comp1").physics("vseq").create("dir3", "DirichletBoundary", 2);
    model.component("comp1").physics("vseq").feature("dir3").selection().set(3);
    model.component("comp1").physics("vseq").create("dir4", "DirichletBoundary", 2);
    model.component("comp1").physics("vseq").feature("dir4").selection().set(1, 12);
    model.component("comp1").physics("vseq").create("weak1", "WeakContribution", 2);
    model.component("comp1").physics("vseq").feature("weak1").selection().set(6, 15);
    model.component("comp1").physics("vseq").label("Solid Velocity");
    model.component("comp1").physics("vseq").feature("wfeq1").set("weak", new String[][]{{"vsWC"}, {"0"}, {"0"}});
    model.component("comp1").physics("vseq").feature("dir1").set("useDirichletCondition", new int[][]{{1}, {0}, {0}});
    model.component("comp1").physics("vseq").feature("dir1").label("X edge");
    model.component("comp1").physics("vseq").feature("dir2").set("useDirichletCondition", new int[][]{{0}, {1}, {0}});
    model.component("comp1").physics("vseq").feature("dir2").label("Y edge");
    model.component("comp1").physics("vseq").feature("dir3").set("useDirichletCondition", new int[][]{{0}, {0}, {1}});
    model.component("comp1").physics("vseq").feature("dir3").label("Z edge");
    model.component("comp1").physics("vseq").feature("dir4").set("useDirichletCondition", new int[][]{{1}, {0}, {0}});
    model.component("comp1").physics("vseq").feature("dir4").label("Symmetry");
    model.component("comp1").physics("vseq").feature("weak1").set("weakExpression", "TBC");
    model.component("comp1").physics("vseq").feature("weak1").label("Traction Continuity");
    
    //mesh displacement
    model.component("comp1").physics().create("umeq", "WeakFormPDE", "geom1");
    model.component("comp1").physics("umeq").identifier("umeq");
    model.component("comp1").physics("umeq").field("dimensionless").field("um");
    model.component("comp1").physics("umeq").field("dimensionless").component(new String[]{"umx", "umy", "umz"});
    model.component("comp1").physics("umeq").prop("Units").set("DependentVariableQuantity", "displacement");
    model.component("comp1").physics("umeq").selection().set(2);
    model.component("comp1").physics("umeq").create("dir2", "DirichletBoundary", 2);
    model.component("comp1").physics("umeq").feature("dir2").selection().set(7);
    model.component("comp1").physics("umeq").create("dir4", "DirichletBoundary", 2);
    model.component("comp1").physics("umeq").feature("dir4").selection().set(4, 11);
    model.component("comp1").physics("umeq").create("dir5", "DirichletBoundary", 2);
    model.component("comp1").physics("umeq").feature("dir5").selection().set(9);
    model.component("comp1").physics("umeq").create("dir6", "DirichletBoundary", 2);
    model.component("comp1").physics("umeq").feature("dir6").selection().set(6, 15);
    model.component("comp1").physics("umeq").create("dir7", "DirichletBoundary", 2);
    model.component("comp1").physics("umeq").feature("dir7").selection().set(8);
    model.component("comp1").physics("umeq").create("dir8", "DirichletBoundary", 2);
    model.component("comp1").physics("umeq").feature("dir8").selection().set(17);
    model.component("comp1").physics("umeq").create("dir9", "DirichletBoundary", 2);
    model.component("comp1").physics("umeq").feature("dir9").selection().set(10);
    model.component("comp1").physics("umeq").label("Mesh displacement");
    model.component("comp1").physics("umeq").feature("wfeq1")
         .set("weak", new String[][]{{"umWC/Jm^2"}, {"0"}, {"0"}});
    model.component("comp1").physics("umeq").feature("dir2").label("Skull");
    model.component("comp1").physics("umeq").feature("dir4").set("useDirichletCondition", new int[][]{{1}, {0}, {0}});
    model.component("comp1").physics("umeq").feature("dir4").label("Symmetry");
    model.component("comp1").physics("umeq").feature("dir5")
         .set("r", new String[][]{{"-urad*nx"}, {"-urad*ny"}, {"-urad*nz"}});
    model.component("comp1").physics("umeq").feature("dir5").label("Dilation/Pulsation");
    model.component("comp1").physics("umeq").feature("dir6").set("r", new String[][]{{"usx"}, {"usy"}, {"usz"}});
    model.component("comp1").physics("umeq").feature("dir6").label("Displacement continuity");
    model.component("comp1").physics("umeq").feature("dir7").set("useDirichletCondition", new int[][]{{0}, {0}, {1}});
    model.component("comp1").physics("umeq").feature("dir7").label("Into Brain");
    model.component("comp1").physics("umeq").feature("dir8").set("useDirichletCondition", new int[][]{{1}, {0}, {0}});
    model.component("comp1").physics("umeq").feature("dir8").label("X edge");
    model.component("comp1").physics("umeq").feature("dir9").set("r", new String[][]{{"urad*x/Ro"}, {"urad*y/Ro"}, {"0"}});
    model.component("comp1").physics("umeq").feature("dir9").label("Dilation/Pulsation1");
    
    //fluid velocity
    model.component("comp1").physics().create("vfeq", "WeakFormPDE", "geom1");
    model.component("comp1").physics("vfeq").identifier("vfeq");
    model.component("comp1").physics("vfeq").field("dimensionless").field("vf");
    model.component("comp1").physics("vfeq").field("dimensionless").component(new String[]{"vfx", "vfy", "vfz"});
    model.component("comp1").physics("vfeq").prop("Units").set("DependentVariableQuantity", "velocity");
    model.component("comp1").physics("vfeq").selection().set(2);
    model.component("comp1").physics("vfeq").create("dir1", "DirichletBoundary", 2);
    model.component("comp1").physics("vfeq").feature("dir1").selection().set(7);
    model.component("comp1").physics("vfeq").create("dir2", "DirichletBoundary", 2);
    model.component("comp1").physics("vfeq").feature("dir2").selection().set(4, 11);
    model.component("comp1").physics("vfeq").create("dir3", "DirichletBoundary", 2);
    model.component("comp1").physics("vfeq").feature("dir3").selection().set(9);
    model.component("comp1").physics("vfeq").create("dir4", "DirichletBoundary", 2);
    model.component("comp1").physics("vfeq").feature("dir4").selection().set(6, 15);
    model.component("comp1").physics("vfeq").create("dir5", "DirichletBoundary", 2);
    model.component("comp1").physics("vfeq").feature("dir5").selection().set(8);
    model.component("comp1").physics("vfeq").create("weak1", "WeakContribution", 2);
    model.component("comp1").physics("vfeq").feature("weak1").selection().set(14);
    model.component("comp1").physics("vfeq").create("dir6", "DirichletBoundary", 2);
    model.component("comp1").physics("vfeq").feature("dir6").selection().set(17);
    model.component("comp1").physics("vfeq").create("dir7", "DirichletBoundary", 2);
    model.component("comp1").physics("vfeq").feature("dir7").selection().set(10);
    model.component("comp1").physics("vfeq").label("Fluid momentum");
    model.component("comp1").physics("vfeq").feature("wfeq1").set("weak", new String[][]{{"vfWC"}, {"0"}, {"0"}});
    model.component("comp1").physics("vfeq").feature("dir1").label("Skull");
    model.component("comp1").physics("vfeq").feature("dir2").set("useDirichletCondition", new int[][]{{1}, {0}, {0}});
    model.component("comp1").physics("vfeq").feature("dir2").label("Symmetry");
    model.component("comp1").physics("vfeq").feature("dir3")
         .set("r", new String[][]{{"-vrad*nx"}, {"-vrad*ny"}, {"-vrad*nz"}});
    model.component("comp1").physics("vfeq").feature("dir3").label("Dilation/Pulsation");
    model.component("comp1").physics("vfeq").feature("dir4").set("r", new String[][]{{"vsx"}, {"vsy"}, {"vsz"}});
    model.component("comp1").physics("vfeq").feature("dir4").label("Velocity continuity");
    model.component("comp1").physics("vfeq").feature("dir5").set("useDirichletCondition", new int[][]{{0}, {0}, {1}});
    model.component("comp1").physics("vfeq").feature("dir5").label("Into Brain");
    model.component("comp1").physics("vfeq").feature("weak1").set("weakExpression", "TBCf*p1*step3(t/teq)");
    model.component("comp1").physics("vfeq").feature("weak1").label("SAS pressure diff");
    model.component("comp1").physics("vfeq").feature("dir6").set("useDirichletCondition", new int[][]{{1}, {0}, {0}});
    model.component("comp1").physics("vfeq").feature("dir6").label("X edge");
    model.component("comp1").physics("vfeq").feature("dir7").set("r", new String[][]{{"vrad*x/Ro"}, {"vrad*y/Ro"}, {"0"}});
    model.component("comp1").physics("vfeq").feature("dir7").label("Dilation/Pulsation1");

    
    // fluid pressure
    model.component("comp1").physics().create("pfeq", "WeakFormPDE", "geom1");
    model.component("comp1").physics("pfeq").identifier("w5");
    model.component("comp1").physics("pfeq").field("dimensionless").field("pf");
    model.component("comp1").physics("pfeq").field("dimensionless").component(new String[]{"pf"});
    model.component("comp1").physics("pfeq").prop("Units").set("DependentVariableQuantity", "pressure");
    model.component("comp1").physics("pfeq").selection().set(2);
    model.component("comp1").physics("pfeq").label("Fluid incompressibility");
    model.component("comp1").physics("pfeq").prop("ShapeProperty").set("order", 1);
    model.component("comp1").physics("pfeq").feature("wfeq1").set("weak", "pfWC");
	
	// study
	// Take value from args to determine the time for the study
	float tSpan = (float)10.0;
	float tStep = (float)0.05;
	int nSteps = (int)(tSpan/tStep) + 1; //steps excluding 0
	float tStart = (float)0.5;
	float bdfStep = (float) 0.005;
	float tStop;
	
	float cStart = arg.charAt(0);
	cStart = cStart - (float)65.0; // ascii value for A
	tStart = tStart + tSpan*cStart;
	tStop = tStart + tSpan;
	System.out.println("Running Simulation from " +tStart + " to " +tStop);
	
	String tString = "0,range(" + tStart + "," + tStep +"," + tStop +")"; 
	
    model.study().create("std1");
    model.study("std1").create("time", "Transient");
    model.study("std1").feature("time").set("tlist", tString);

    model.sol().create("sol1");
    model.sol("sol1").study("std1");
    model.sol("sol1").attach("std1");
    model.sol("sol1").create("st1", "StudyStep");
    model.sol("sol1").create("v1", "Variables");
    model.sol("sol1").create("t1", "Time");
    model.sol("sol1").feature("t1").create("fc1", "FullyCoupled");
    model.sol("sol1").feature("t1").feature().remove("fcDef");
    

    model.sol("sol1").attach("std1");
    model.sol("sol1").feature("v1").set("clist", new String[]{tString, bdfStep + "[s]"});
    model.sol("sol1").feature("t1").set("tlist", tString);
    model.sol("sol1").feature("t1").set("fieldselection", "comp1_umx");
    model.sol("sol1").feature("t1")
         .set("atolmethod", new String[]{"comp1_umx", "global", "comp1_umy", "global", "comp1_umz", "global", "comp1_usx", "global", "comp1_usy", "global", 
         "comp1_usz", "global", "comp1_vsx", "global", "comp1_vsy", "global", "comp1_vsz", "global", "comp1_vfx", "global", 
         "comp1_vfy", "global", "comp1_vfz", "global", "comp1_pf", "global"});
    model.sol("sol1").feature("t1")
         .set("atolvaluemethod", new String[]{"comp1_umx", "factor", "comp1_umy", "factor", "comp1_umz", "factor", "comp1_usx", "factor", "comp1_usy", "factor", 
         "comp1_usz", "factor", "comp1_vsx", "factor", "comp1_vsy", "factor", "comp1_vsz", "factor", "comp1_vfx", "factor", 
         "comp1_vfy", "factor", "comp1_vfz", "factor", "comp1_pf", "factor"});
    model.sol("sol1").feature("t1")
         .set("atolfactor", new String[]{"comp1_umx", "0.1", "comp1_umy", "0.1", "comp1_umz", "0.1", "comp1_usx", "0.1", "comp1_usy", "0.1", 
         "comp1_usz", "0.1", "comp1_vsx", "0.1", "comp1_vsy", "0.1", "comp1_vsz", "0.1", "comp1_vfx", "0.1", 
         "comp1_vfy", "0.1", "comp1_vfz", "0.1", "comp1_pf", "0.1"});
    model.sol("sol1").feature("t1")
         .set("atol", new String[]{"comp1_umx", "1e-3", "comp1_umy", "1e-3", "comp1_umz", "1e-3", "comp1_usx", "1e-3", "comp1_usy", "1e-3", 
         "comp1_usz", "1e-3", "comp1_vsx", "1e-3", "comp1_vsy", "1e-3", "comp1_vsz", "1e-3", "comp1_vfx", "1e-3", 
         "comp1_vfy", "1e-3", "comp1_vfz", "1e-3", "comp1_pf", "1e-3"});
    model.sol("sol1").feature("t1")
         .set("atoludot", new String[]{"comp1_umx", "1e-3", "comp1_umy", "1e-3", "comp1_umz", "1e-3", "comp1_usx", "1e-3", "comp1_usy", "1e-3", 
         "comp1_usz", "1e-3", "comp1_vsx", "1e-3", "comp1_vsy", "1e-3", "comp1_vsz", "1e-3", "comp1_vfx", "1e-3", 
         "comp1_vfy", "1e-3", "comp1_vfz", "1e-3", "comp1_pf", "1e-3"});
    model.sol("sol1").feature("t1").set("initialstepbdf", bdfStep);
    model.sol("sol1").feature("t1").set("initialstepbdfactive", true);
    model.sol("sol1").feature("t1").set("maxstepconstraintbdf", "const");
    model.sol("sol1").feature("t1").set("maxstepbdf", bdfStep);
    model.sol("sol1").feature("t1").set("consistent", false);
    model.sol("sol1").feature("t1").set("estrat", "exclude");
    // model.sol("sol1").runFromTo("st1", "v1"); // to initialize the model 
    model.sol("sol1").runAll(); // to run the whole model
    
	//results
	
	
    model.result().create("pg1", "PlotGroup3D");
    model.result("pg1").create("arws1", "ArrowSurface");
    model.result("pg1").feature("arws1").set("expr", new String[]{"-nx", "-ny", "-nz"});
    model.result("pg1").feature("arws1").set("descr", "");
    model.result("pg1").feature("arws1").set("scale", 8.274912695491622);
    model.result("pg1").feature("arws1").set("arrowcount", 2000);
    model.result("pg1").feature("arws1").set("scaleactive", false);
    
    
    model.result().create("pg2", "PlotGroup1D");
    model.result("pg2").create("glob1", "Global");
    model.result("pg2").set("xlabel", "Time");
    model.result("pg2").set("ylabel", "Integration 1 (um<sup>3</sup>/s)");
    model.result("pg2").set("xlabelactive", false);
    model.result("pg2").set("ylabelactive", false);
    model.result("pg2").feature("glob1").set("expr", new String[]{"Outflow(flow)"});
    model.result("pg2").feature("glob1").set("unit", new String[]{"um^3/s"});
    model.result("pg2").feature("glob1").set("descr", new String[]{"Integration 1"});
    
    
    //export
    String exportRange = "range(2,1," + nSteps + ")";
    String resultPath = modelPath + "/Particle_tracking";
    String gridPath = modelPath + "/Grids/";
    
    model.result().export().create("data1", "Data");
    model.result().export().create("data2", "Data");
    model.result().export().create("data3", "Data");
    model.result().export().create("data4", "Data");
    model.result().export().create("data5", "Data");
    model.result().export().create("data6", "Data");
    
    model.result().export("data1").label("Mesh Displacement");
    model.result().export("data1").set("looplevelinput", new String[]{"manualindices"});
    model.result().export("data1").set("looplevelindices", new String[]{exportRange});
    model.result().export("data1").set("expr", new String[]{"umx", "umy", "umz"});
    model.result().export("data1").set("unit", new String[]{"\u00b5m", "\u00b5m", "\u00b5m"});
    model.result().export("data1").set("descr", new String[]{"", "", ""});
    model.result().export("data1").set("filename", resultPath + "/Mesh_displacement"+ arg +".txt");
    model.result().export("data1").set("location", "file");
    model.result().export("data1").set("coordfilename", gridPath + "/Full_grid.txt");
    model.result().export("data1").set("header", false);
    
    model.result().export("data2").label("Fluid Velocity");
    model.result().export("data2").set("looplevelinput", new String[]{"manualindices"});
    model.result().export("data2").set("looplevelindices", new String[]{exportRange});
    model.result().export("data2").set("expr", new String[]{"xcdotx", "xcdoty", "xcdotz"});
    model.result().export("data2").set("unit", new String[]{"um/s", "um/s", "um/s"});
    model.result().export("data2").set("descr", new String[]{"", "", ""});
    model.result().export("data2").set("filename", resultPath + "/Fluid_velocity"+ arg +".txt");
    model.result().export("data2").set("location", "file");
    model.result().export("data2").set("coordfilename", gridPath + "/Full_grid.txt");
    model.result().export("data2").set("header", false);
    
    model.result().export("data3").label("Vessel Wall");
    model.result().export("data3").set("looplevelinput", new String[]{"manualindices"});
    model.result().export("data3").set("looplevelindices", new String[]{exportRange});
    model.result().export("data3").set("expr", new String[]{"umx", "umy", "umz"});
    model.result().export("data3").set("unit", new String[]{"\u00b5m", "\u00b5m", "\u00b5m"});
    model.result().export("data3").set("descr", new String[]{"", "", ""});
    model.result().export("data3").set("filename", resultPath + "/Vessel_wall_displacement"+ arg +".txt");
    model.result().export("data3").set("location", "file");
    model.result().export("data3").set("coordfilename",gridPath + "Vessel_wall_grid.txt");
    model.result().export("data3").set("header", false);
    
    model.result().export("data4").label("SAS Wall");
    model.result().export("data4").set("looplevelinput", new String[]{"manualindices"});
    model.result().export("data4").set("looplevelindices", new String[]{exportRange});
    model.result().export("data4").set("expr", new String[]{"umx", "umy", "umz"});
    model.result().export("data4").set("unit", new String[]{"\u00b5m", "\u00b5m", "\u00b5m"});
    model.result().export("data4").set("descr", new String[]{"", "", ""});
    model.result().export("data4").set("filename", resultPath + "/SAS_wall_displacement"+ arg +".txt");
    model.result().export("data4").set("location", "file");
    model.result().export("data4").set("coordfilename", gridPath + "/SAS_wall_grid.txt");
    model.result().export("data4").set("header", false);
    
    model.result().export("data5").label("Brain PVS Interface");
    model.result().export("data5").set("looplevelinput", new String[]{"manualindices"});
    model.result().export("data5").set("looplevelindices", new String[]{exportRange});
    model.result().export("data5").set("expr", new String[]{"umx", "umy", "umz"});
    model.result().export("data5").set("unit", new String[]{"\u00b5m", "\u00b5m", "\u00b5m"});
    model.result().export("data5").set("descr", new String[]{"", "", ""});
    model.result().export("data5").set("filename", resultPath + "/Brain_PVS_displacement"+ arg +".txt");
    model.result().export("data5").set("location", "file");
    model.result().export("data5").set("coordfilename", gridPath + "/Brain_PVS_grid.txt");
    model.result().export("data5").set("header", false);
    
    model.result().export("data6").label("Brain wall");
    model.result().export("data6").set("looplevelinput", new String[]{"manualindices"});
    model.result().export("data6").set("looplevelindices", new String[]{exportRange});
    model.result().export("data6").set("expr", new String[]{"usx", "usy", "usz"});
    model.result().export("data6").set("unit", new String[]{"\u00b5m", "\u00b5m", "\u00b5m"});
    model.result().export("data6").set("descr", new String[]{"", "", ""});
    model.result().export("data6").set("filename", resultPath + "/Tissue_wall_displacement"+ arg +".txt");
    model.result().export("data6").set("location", "file");
    model.result().export("data6").set("coordfilename", gridPath + "/Tissue_wall_grid.txt");
    model.result().export("data6").set("header", false);
    
    // export the results automatically
    model.result().export("data1").run();
    model.result().export("data2").run();
    model.result().export("data3").run();
    model.result().export("data4").run();
    model.result().export("data5").run();
    model.result().export("data6").run();

    return model;
  }

  public static void main(String[] args) {
	if ((args==null) || (args.length==0)) {
		System.out.println("No arguments given");
		Model model = run("A");
	}
	else{
		Model model = run(args[0]);
	}
  }

}
