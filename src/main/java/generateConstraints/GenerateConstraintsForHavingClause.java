package generateConstraints;

import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.AggregateFunction;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

/**
 * Contains methods to generates constraints for the having clause of the query block
 * @author mahesh
 *
 */
public class GenerateConstraintsForHavingClause {

	private static Logger logger = Logger.getLogger(GenerateConstraintsForHavingClause.class.getName());
	public static String getHavingClauseConstraints(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node havingClause, int totalRows, int groupNumber) throws Exception{
		
		/**If there is no having clause*/
		if(havingClause == null || havingClause.getType() == null){
			return "";
		}
		else{
			String returnStr = "";
			returnStr = getCVCForHavingConstraintRepeated(cvc, queryBlock, havingClause,totalRows,groupNumber);
			if(returnStr.equalsIgnoreCase(""))
				return "";
			else
				return returnStr ;//+= ";";
		}
	}
	
	
	
	/**FIXME: Write good doc for this function*/
	/**
	 * Generate Having Clause constraints for a given group number
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @param totalRows
	 * @param paramId: This is not used in this function, then why did this was there??
	 * @param groupNumber
	 * @return
	 * @throws Exception
	 */
	public static String getCVCForHavingConstraintRepeated(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n, int totalRows, int groupNumber) throws Exception {

		ConstraintGenerator consGen = new ConstraintGenerator();
		if(n.getType().equalsIgnoreCase(Node.getBroNodeType())){
			String returnStr = "";
			if(n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())){
				
				if(n.getLeft().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMAX())){
					/*returnStr = "MAX: TYPE = SUBTYPE(LAMBDA(x: INT): " + " x " + n.getOperator() + n.getRight().toCVCString(10, queryBlock.getParamMap());
					if(n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")){ 
						returnStr += " AND x > 0 );";
					}
					else if(n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")){
						returnStr += " AND x < 10000000 );";
					}
					else{//operator is = or /=
						returnStr += ");";
					}*/
					returnStr += consGen.getMaxConstraint(queryBlock, n,true);// n.getRight().toCVCString(10, queryBlock.getParamMap()), 
					return returnStr + getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getLeft(), totalRows,groupNumber);
				}
				else if(n.getLeft().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMIN())){
					/*returnStr = "MIN: TYPE = SUBTYPE(LAMBDA(x: INT): " + " x " + n.getOperator() + n.getRight().toCVCString(10, queryBlock.getParamMap());
					if(n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")){ 
						returnStr += " AND x > 0 );";
					}
					else if(n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")){
						returnStr += " AND x < 10000000 );";
					}
					else{//operator is = or /=
						returnStr += ");";
					}*/
					returnStr += consGen.getMinConstraint(queryBlock, n,true);// n.getRight().toCVCString(10, queryBlock.getParamMap())
					return returnStr + getCVCForHavingConstraintRepeated(cvc, queryBlock,  n.getLeft(), totalRows, groupNumber);
				}
				else if(n.getLeft().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggCOUNT())){
					return "";
				}
				else //n.getRight().toCVCString(10, queryBlock.getParamMap()),
					return consGen.getAssertConstraint(getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getLeft(), totalRows,   groupNumber),
							n.getOperator(),
							getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getRight(), totalRows,  groupNumber));//n.getLeft().toCVCString(10, queryBlock.getParamMap())				
			}
			else if(n.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())){
				if(n.getRight().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMAX())){
					/*returnStr = "MAX: TYPE = SUBTYPE(LAMBDA(x: INT): " + n.getLeft().toCVCString(10, queryBlock.getParamMap()) + " " + n.getOperator() + " x ";
					if(n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")){
						returnStr += " AND x < 10000000 );";
					}
					else if(n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")){
						returnStr += " AND x > 0 );";
					}
					else{//operator is = or /=
						returnStr += ";";
					}*/
					returnStr += consGen.getMaxConstraint(queryBlock, n,false);//n.getLeft().toCVCString(10, queryBlock.getParamMap()),
					return returnStr + getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getRight(), totalRows,   groupNumber);
				}
				else if(n.getRight().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMIN())){
					/*returnStr = "MIN: TYPE = SUBTYPE(LAMBDA(x: INT): " + n.getLeft().toCVCString(10, queryBlock.getParamMap()) + " " + n.getOperator() + " x ";
					if(n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")){
						returnStr += " AND x < 10000000 );";
					}
					else if(n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")){
						returnStr += " AND x > 0 );";
					}
					else{//operator is = or /=
						returnStr += ");";
					}*/
					
					returnStr += consGen.getMinConstraint(queryBlock, n,false);// n.getLeft().toCVCString(10, queryBlock.getParamMap()),
					return returnStr + getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getRight(), totalRows,  groupNumber);
				}
				else if(n.getRight().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggCOUNT())){
					return "";
				}
				else 
					return consGen.getAssertConstraint(getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getLeft(), totalRows,  groupNumber),
							n.getOperator(),
							getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getRight(),totalRows,  groupNumber));				
			}
			else{
				logger.log(Level.INFO,"Not an Aggregation!!");
				return "";
			}
		}
		else if(n.getType().equalsIgnoreCase(Node.getValType())){
			String constVal = n.getStrConst();
			if( queryBlock.getParamMap().get(constVal) == null )
				return n.getStrConst();
			else return queryBlock.getParamMap().get(constVal);
		}
		else if(n.getType().equalsIgnoreCase(Node.getAggrNodeType())){
			//Column aggColumn = n.getAgg().getAggCol();
			AggregateFunction af = n.getAgg();
			if(n.getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggAVG())){
				String returnStr = "";//"\nASSERT (";
				//Actual count required my this table
				//Required to adapt when aggregation column has x tuples but total tuples in output are y >= x
				//int myCount = af.getNoOfOutputTuples(noOfOutputTuples);
				String innerTableNo=af.getAggExp().getTableNameNo();
				if(innerTableNo == null){
					innerTableNo = getTableNameNoForBAONode(af.getAggExp());
				
				}
				int myCount = cvc.getNoOfTuples().get(innerTableNo);
				int multiples = totalRows/myCount;
				int extras = totalRows%myCount;

				int offset = cvc.getRepeatedRelNextTuplePos().get(innerTableNo)[1];
				boolean isDistinct=af.isDistinct();

				/*for(int i=1,j=0;i<=myCount;i++,j++){

					int tuplePos=(groupNumber)*myCount+i;

					if(j<extras)
						returnStr += (multiples+1)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos+offset-1+"")+")";
					else
						returnStr += (multiples)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos+offset-1+"")+")";

					if(i<myCount){
						returnStr += "+";
					}
				}
				return returnStr + ") / "+totalRows + " ";*/
				return consGen.getAVGConstraint(myCount,groupNumber,multiples,totalRows,af.getAggExp(), offset);
			}
			else if(n.getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggSUM())){
				String returnStr = "";//\nASSERT ";
				//Actual count required my this table
				
				String innerTableNo=af.getAggExp().getTableNameNo();
				if(innerTableNo == null){
					innerTableNo = getTableNameNoForBAONode(af.getAggExp());
				
				}
				int myCount = cvc.getNoOfTuples().get(innerTableNo);
				int multiples = totalRows/myCount;
				int extras = totalRows%myCount;

				int offset = cvc.getRepeatedRelNextTuplePos().get(innerTableNo)[1];
				boolean isDistinct=af.isDistinct();
				

			/*	for(int i=1,j=0;i<=myCount;i++,j++){
					int tuplePos=(groupNumber)*myCount+i;

					if(j<extras)
						returnStr += (multiples+1)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos+offset-1+"")+")";
					else
						returnStr += (multiples)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos+offset-1+"")+")";

					if(i<myCount){
						returnStr += "+";
					}
				}*/
				
				//return returnStr;
				return consGen.getSUMConstraint(myCount,groupNumber,multiples,totalRows,af.getAggExp(), offset);
			}
			else if(n.getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMAX())){
				String innerTableNo=af.getAggExp().getTableNameNo();
				if(innerTableNo == null){
					innerTableNo = getTableNameNoForBAONode(af.getAggExp());
				}
				return consGen.getMaxAssertConstraint(innerTableNo,groupNumber,totalRows,cvc,af);
			}
			else if(n.getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMIN())){
				String innerTableNo=af.getAggExp().getTableNameNo();
				if(innerTableNo == null){
					innerTableNo = getTableNameNoForBAONode(af.getAggExp());
				}
				return consGen.getMinAssertConstraint(innerTableNo,groupNumber,totalRows,cvc,af);
			}
			else return ""; //TODO: Code for COUNT
		}		
		else return ""; //TODO: Code for Binaty Arithmetic Operator. This will be required in case of complex (arbitrary) having clauses.
	}
	
	
	/**
	 * Get the table name number from the BAO node. It contains expression with a column
	 * So traverse and find the table name number of the column on which expression is given 
	 * 
	 * @param Node - BAONode
	 * @return
	 */
	public static String getTableNameNoForBAONode(Node n1) {
		
		if(n1.getRight() != null && n1.getRight().getTableNameNo() != null){
			return n1.getRight().getTableNameNo();
		}	
		else if(n1.getLeft() != null && n1.getLeft().getTableNameNo() != null){
			return n1.getLeft().getTableNameNo();	
		}
		else {
			if(n1.getLeft() != null){
				return getTableNameNoForBAONode(n1.getLeft());
			}else if(n1.getRight() != null){
				return getTableNameNoForBAONode(n1.getRight());
			}
		}
		return null;
		
	}
	
	public static Node getValueForExpressionsInAgg(AggregateFunction af){
		//String iVal = "i";
		
		Node newAgg = af.getAggExp();
		
		if(af.getAggExp().getType().equalsIgnoreCase(Node.getBaoNodeType())){
			newAgg = replaceIValue(af.getAggExp());
		}
		return newAgg;
		
	}
	
	public static Node replaceIValue(Node n1){
		Node nd = new Node(n1);
		
		
		if(nd.getRight() != null && nd.getRight().getTableNameNo() != null){
			
			nd.getRight().setTableNameNo("");
			nd.getRight().getColumn().setColumnName("i");
			nd.getRight().setType("i");
			return nd;
		}	
		else if(nd.getLeft() != null && nd.getLeft().getTableNameNo() != null){
			nd.getLeft().setTableNameNo("");
			nd.getLeft().setTableNameNo("");
			nd.getLeft().getColumn().setColumnName("i");
			nd.getLeft().setType("i");
			return nd;
		}
		else {
			if(nd.getLeft() != null){
				nd.setLeft(replaceIValue(nd.getLeft()));
				return nd;
			}else if(nd.getRight() != null){
				nd.setRight(replaceIValue(nd.getRight()));
				return nd;
			}
		}
		return nd;
	}
	

}
