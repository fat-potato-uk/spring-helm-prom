package demo.models.exceptions;


import static java.lang.String.format;

public class EmployeeNotFoundException extends Exception {

    public EmployeeNotFoundException(Long id) {
        super(format("No Employee found with ID: %d", id));
    }
}
