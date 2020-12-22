package demo.controllers;


import demo.managers.EmployeeManager;
import demo.models.Employee;
import demo.models.exceptions.EmployeeNotFoundException;
import demo.repositories.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {

    @MockBean
    private EmployeeRepository employeeRepository;

    @MockBean
    private EmployeeManager employeeManager;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllEmployeesTest() throws Exception {
        when(employeeManager.getAll()).thenReturn(List.of(new Employee("Bilbo Baggins", "burglar"),
                                                          new Employee("Frodo Baggins", "thief")));

        this.mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bilbo Baggins"))
                .andExpect(jsonPath("$[0].role").value("burglar"))
                .andExpect(jsonPath("$[1].name").value("Frodo Baggins"))
                .andExpect(jsonPath("$[1].role").value("thief"))
                .andDo(print()); // Handy for those of us who are rubbish at working out what Json should look like.
    }

    @Test
    void createNewEmployeeTest() throws Exception {
        var employee = new Employee("Harry Potter", "Rubbish Wizard");
        when(employeeManager.create(eq(employee))).thenReturn(employee);

        this.mockMvc.perform(post("/employees")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content("{\"name\":\"Harry Potter\",\"role\":\"Rubbish Wizard\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Harry Potter"))
                .andExpect(jsonPath("$.role").value("Rubbish Wizard"));
    }

    @Test
    void getOneEmployeeTest() throws Exception {
        when(employeeManager.getEmployee(eq(1L))).thenReturn(new Employee("Bilbo Baggins", "burglar"));
        this.mockMvc.perform(get("/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bilbo Baggins"))
                .andExpect(jsonPath("$.role").value("burglar"));
    }

    @Test
    void getOneEmployeeNotFoundTest() throws Exception {
        when(employeeManager.getEmployee(anyLong())).thenThrow(new EmployeeNotFoundException(99L));
        this.mockMvc.perform(get("/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No Employee found with ID: 99"));
    }

    @Test
    void replaceOrCreateEmployee() throws Exception {
        var employee = new Employee("Harry Potter", "Rubbish Wizard");
        when(employeeManager.replaceOrCreateEmployee(eq(10L), eq(employee))).thenReturn(employee);

        this.mockMvc.perform(put("/employees/10")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content("{\"name\":\"Harry Potter\",\"role\":\"Rubbish Wizard\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Harry Potter"))
                .andExpect(jsonPath("$.role").value("Rubbish Wizard"));
    }

    @Test
    void deleteEmployee() throws Exception {
        this.mockMvc.perform(delete("/employees/123")).andExpect(status().isOk());
        verify(employeeManager, times(1)).removeEmployee(123L);
    }
}
