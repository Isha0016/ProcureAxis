package com.fsc.supply_chain_common_operations.exceptions;

public class DuplicateRecordException extends RuntimeException{
    public DuplicateRecordException(String message){
        super(message);
    }
}
