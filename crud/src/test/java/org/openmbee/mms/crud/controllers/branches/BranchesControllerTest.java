package org.openmbee.mms.crud.controllers.branches;

import org.junit.Test;

import static org.junit.Assert.*;

public class BranchesControllerTest {

    private void checkFail(String id){
        assertFalse(BranchesController.isBranchIdValid(id));
    }

    private void checkGood(String id){
        assertTrue(BranchesController.isBranchIdValid(id));
    }

    @Test
    public void isProjectIdValid() {
        checkGood("branch-1234");
        checkGood("_A-branch");
        checkGood("000000000000000000000");
        checkGood("branch-with65characters000000000000000000000000000000000000000000");
        checkFail("branch with spaces");
        checkFail("specialcharacters!");
    }
}