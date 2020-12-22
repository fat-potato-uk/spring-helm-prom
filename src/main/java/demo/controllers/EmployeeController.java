package demo.controllers;

import java.util.List;

import demo.managers.EmployeeManager;
import demo.models.Employee;
import demo.models.exceptions.EmployeeNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE, value = "/employees")
class EmployeeController {

    @Autowired
    private EmployeeManager employeeManager;

    @GetMapping("")
    List<Employee> all() {
        return employeeManager.getAll();
    }

    @PostMapping("")
    Employee newEmployee(@RequestBody Employee newEmployee) {
        return employeeManager.create(newEmployee);
    }

    @GetMapping("/{id}")
    Employee getEmployee(@PathVariable Long id) throws EmployeeNotFoundException {
        return employeeManager.getEmployee(id);
    }

    @PutMapping("/{id}")
    Employee replaceOrCreateEmployee(@RequestBody Employee newEmployee, @PathVariable Long id) {
        return employeeManager.replaceOrCreateEmployee(id, newEmployee);
    }

    @DeleteMapping("/{id}")
    void deleteEmployee(@PathVariable Long id) {
        employeeManager.removeEmployee(id);
    }
}