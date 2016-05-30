/**
 * Created by zhf on 2015/10/28.
 */

/**
 * 词法分析器
 * 功能：识别将源文件中各种字符，将其属性、值和所在的行号存储到文件lex.txt中
 * 实现方式：LexAnalysis lex=newLexAnalysis(filename).bAnalysis(),filename是源文件存储位置
 *
 *
 */

import java.io.*;
public class LexAnalysis {//keyWord={"program","begin","end","if","then","else","const","procedure","var","do","while","call","read","write","repeat","until"};
    private static  int PROG=1;//program
    private static  int BEG=2;//begin
    private static  int END=3;//end
    private static  int IF=4;//if
    private static  int THEN=5;//then
    private static  int ELS=6;//else
    private static  int CON=7;//const
    private static  int PROC=8;//procdure
    private static  int VAR=9;//var
    private static  int DO=10;//do
    private static  int WHI=11;//while
    private static  int CAL=12;//call
    private static  int REA=13;//read
    private static  int WRI=14;//write
    private static  int REP=15;//repeate
    private static  int ODD=16;//  ODDl      和keyWord中每个字的序号是相等的


    private static  int EQU=17;//"="
    private static  int LES=18;//"<"
    private static  int LESE=19;//"<="
    private static  int LARE=20;//">="
    private static  int LAR=21;//">"
    private static  int NEQE=22;//"<>"


    private static  int ADD=23;//"+"
    private static  int SUB=24;//"-"
    private static  int MUL=25;//"*"
    private static  int DIV=26;//"/"

    private static  int SYM=27;//标识符
    private static  int CONST=28;//常量

    private static  int CEQU=29;//":="

    private static  int COMMA=30;//","
    private static  int SEMIC=31;//";"
    private static  int POI=32;//"."
    private static  int LBR=33;//"("
    private static  int RBR=34;//")"


    private ChTable ct=new ChTable();
    private RValue rv=new RValue();
    private String[] keyWord=ct.getKeyWord();
    private String[] symTable=ct.getSymTable();
    private int symLength=symTable.length;
    private String[] constTable=ct.getConstTable();
    private int conLength=constTable.length;

    private char ch=' ';
    private String strToken;
    private  String filename;
    private char[] buffer;
    private int searchPtr=0;
    private int line=1;
    private boolean errorHappen=false;

    public LexAnalysis(String _filename){
        for(int i=0;i<symLength;i++){
            symTable[i]=null;
        }
        for(int j=0;j<conLength;j++){
            constTable[j]=null;
        }
        filename=_filename;
    }

    /**
     * 预处理函数：
     * 功能：读取源文件内容到字符数组buffer中去，包括换行符
     * */
    public char[] preManage(){
        File file=new File(filename);
        BufferedReader bf=null;
        try {
            //   System.out.println("read file test.txt...");
            bf=new BufferedReader(new FileReader(file));
            String temp1="",temp2 = "";
            while((temp1=bf.readLine())!=null){
                temp2=temp2+temp1+String.valueOf('\n');

            }
            buffer=temp2.toCharArray();
            bf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return buffer;
    }
    public char getChar(){
        if(searchPtr<buffer.length){
            ch=buffer[searchPtr];
            searchPtr++;
        }
//	System.out.print(ch);
        return ch;
    }


    public void getBC(){
        while( (ch==' '||ch=='	'||ch=='\n')&&(searchPtr<buffer.length)){
            if(ch=='\n'){
                line++;
            }
            getChar();
        }
    }

    public String concat(){
        strToken=strToken+String.valueOf(ch);
        return strToken;
    }

    public boolean isLetter(){
        if(Character.isLetter(ch)){
            return true;
        }
        return false;
    }

    public boolean isDigit(){
        if(Character.isDigit(ch)){
            return true;
        }
        return false;
    }

    public int reserve(){
        for(int i=0;i<keyWord.length;i++){
            if(keyWord[i].equals(strToken)){
                return i+1;
            }
        }
        return 0;
    }

    public void retract(){
        searchPtr--;
        ch=' ';
    }

    public int insertId(){
        for(int i=0;i<symLength;i++){
            if(symTable[i]==null){
                symTable[i]=strToken;
                return i;
            }
        }
        return -1;//表示symTable已经满了
    }

    public int insertConst(){
        for(int i=0;i<conLength;i++){
            if(constTable[i]==null){
                constTable[i]=strToken;
                return i;
            }
        }
        return -1;//constTable已经满了
    }
    //上面的所有函数参考书上的伪码程序即可

    public void showError(){
        System.out.println();
        System.out.print("ERROR: cannot recognize the word in line "+line);
        System.out.println();


    }

    /**
     * 字符识别函数
     * 功能：通过读取buffer数组中的单个字符进行识别源程序中的各个元素，每次识别一个元素
     * @return 识别出的字符的属性：RV.getId()是属性，RV.getValue()是值，RV.getLine()是所在行号
     * */
    public RValue analysis(){
        int code,value;
        strToken="";
        getChar();
        getBC();//经过此步，如果ch=='\n'，则肯定是到达了文件末尾
        if(ch=='\n'){
            rv.setId(-1);
            rv.setValue("-1");
            rv.setLine(line);
            return rv;
        }
        if(isLetter()){
            while((isLetter()||isDigit())){
                concat();
                getChar();
            }
            retract();
            code=reserve();
            if(code==0){
                value=insertId();
                rv.setId(SYM);
                rv.setValue(symTable[value]);
                rv.setLine(line);
                return rv;
            }
            else {
                rv.setId(code);
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }
        }else if(isDigit()){
            while(isDigit()){
                concat();
                getChar();
            }
            retract();
            value=insertConst();
            rv.setId(CONST);
            rv.setValue(constTable[value]);
            rv.setLine(line);
            return rv;
        }else if(ch=='='){
            rv.setId(EQU);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }else if(ch=='+'){
            rv.setId(ADD);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }else if(ch=='-'){
            rv.setId(SUB);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        } else if(ch=='*'){
            rv.setId(MUL);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }else if(ch=='/'){
            rv.setId(DIV);
            rv.setValue("/");
            rv.setLine(line);
            return rv;
        }else if(ch=='<'){
            getChar();
            if(ch=='='){
                rv.setId(LESE);
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }else if(ch=='>'){
                rv.setId(NEQE);
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }else{
                retract();
                rv.setId(LES);
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }
        }else if(ch=='>'){
            getChar();
            if(ch=='='){
                rv.setId(LARE);
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }else{
                retract();
                rv.setId(LAR);
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }
        }else if(ch==','){
            rv.setId(COMMA);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }else if(ch==';'){
            rv.setId(SEMIC);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }else if(ch=='.'){
            rv.setId(POI);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }else if(ch=='('){
            rv.setId(LBR);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }else if(ch==')'){
            rv.setId(RBR);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }else if(ch==':'){
            getChar();
            if(ch=='='){
                rv.setId(CEQU);
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }else{
                retract();
            }
        }
        errorHappen=true;
        return rv;

    }


/**
 * 循环识别出所有字符并输出到文件lex.txt中
 * */
    public void bAnalysis(){
        preManage();
        RValue temp;
        String str="lex.txt";
        OutputStream myout=null;
        File file=new File(str);
        try{
            myout=new FileOutputStream(file);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
        while(searchPtr<buffer.length&&errorHappen==false){
            temp=analysis();

            String tempId=String.valueOf(temp.getId()),tempLine=String.valueOf(temp.getLine());
            byte[] bid=tempId.getBytes();
            byte[] bname=temp.getValue().getBytes();
            byte[] bline=tempLine.getBytes();
            try{
                myout.write(bid);
                myout.write(' ');
                myout.write(bname);
                myout.write(' ');
                myout.write(bline);
                myout.write('\n');
            }catch(IOException e){
                e.printStackTrace();
            }

        }//while
        try{
            myout.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        if(errorHappen==true){
            showError();
        }
    }
}


















