package miniJava.ContextualAnalyzer;

/**
 * Created by NickWu on 2017/3/19.
 */

import miniJava.AbstractSyntaxTrees.*;

import java.util.ArrayList;
import java.util.HashMap;

public class IdTable {

    private HashMap<String, Declaration> currLevelIdTable;
    public ArrayList<HashMap<String, Declaration>> allLevelIdTables;


    // -----------Constructors------------------
    public IdTable() {
        currLevelIdTable = null;
        allLevelIdTables = new ArrayList<>();
    }

    public IdTable(HashMap<String, Declaration> idTable) {
        currLevelIdTable = idTable;
        allLevelIdTables = new ArrayList<>();
        allLevelIdTables.add(currLevelIdTable);
    }

    // -----------Getters-----------------------
    public int getCurrLevel() {
        return allLevelIdTables.size() - 1;
    }

    public HashMap<String, Declaration> getCurrIdTable() {
        return this.currLevelIdTable;
    }

    // ----------Scopes--------------------------
    public void openScope() {
        currLevelIdTable = new HashMap<>();
        allLevelIdTables.add(currLevelIdTable);
    }

    public void openScope(HashMap<String, Declaration> newIdTable) {
        currLevelIdTable = newIdTable;
        allLevelIdTables.add(currLevelIdTable);
    }

    public void closeScope() {
        allLevelIdTables.remove(this.getCurrLevel());
        currLevelIdTable = allLevelIdTables.get(this.getCurrLevel());
    }

    // --------------Decl----------------------
    public Object enterDecl(Declaration declaration) {
        String id = declaration.id.spelling;

            if (currLevelIdTable.containsKey(id)) {
                return null;
            } else {
                currLevelIdTable.put(id, declaration);
            }

//        Declaration decl = this.find(id);
//        if (decl != null) {
//            if (decl instanceof MemberDecl && declaration instanceof MemberDecl) {
//                if (decl == declaration) return null;
//            } else if (declaration.id.spelling.equals("System") || declaration.id.spelling.equals("String")) {
//
//            } else if (!(declaration instanceof LocalDecl && decl instanceof MemberDecl)) return null;
//        }
//
//        currLevelIdTable.put(id, declaration);
        return declaration;
    }

    public Declaration find(String id) {
        for (int i = getCurrLevel(); i >= 0; i--) {
            HashMap<String, Declaration> idTable = allLevelIdTables.get(i);
            if (idTable.get(id) != null) {
                return idTable.get(id);
            }
        }

        return null;
    }

    public Declaration findClass(String id) {
        for (int i = getCurrLevel(); i >= 0; i--) {
            HashMap<String, Declaration> idTable = allLevelIdTables.get(i);
            if (idTable.get(id) != null && idTable.get(id) instanceof ClassDecl) {
                return idTable.get(id);
            }
        }

        return null;
    }

    public Declaration findMethod(String id) {
        for (int i = getCurrLevel(); i >= 0; i--) {
            HashMap<String, Declaration> idTable = allLevelIdTables.get(i);
            if (idTable.get(id) != null && idTable.get(id) instanceof MethodDecl) {
                return idTable.get(id);
            }
        }

        return null;
    }
    
    public Declaration findMember(String id) {
        for (int i = getCurrLevel(); i >= 0; i--) {
            HashMap<String, Declaration> idTable = allLevelIdTables.get(i);
            if (idTable.get(id) != null && (idTable.get(id) instanceof MemberDecl)) {
                return idTable.get(id);
            }
        }

        return null;
    }
    
    public Declaration findMemberOrClass(String id) {
        for (int i = getCurrLevel(); i >= 0; i--) {
            HashMap<String, Declaration> idTable = allLevelIdTables.get(i);
            if (idTable.get(id) != null && (idTable.get(id) instanceof MemberDecl || idTable.get(id) instanceof ClassDecl)) {
                return idTable.get(id);
            }
        }

        return null;
    }

    public boolean existsIdAtCurrentLevel(String id) {
        for (String currId : currLevelIdTable.keySet()) {
            if (id.equals(currId)) return true;
        }

        return false;
    }

    public void switchTable(int ix1, int ix2) {
        HashMap<String, Declaration> ix1Table = allLevelIdTables.get(ix1);
        HashMap<String, Declaration> ix2Table = allLevelIdTables.get(ix2);
        allLevelIdTables.remove(ix1);
        allLevelIdTables.add(ix1, ix2Table);
        allLevelIdTables.remove(ix2);
        allLevelIdTables.add(ix2, ix1Table);
    }

}
