package demo.models;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = {"id", "salary"})
public class Employee {
    @Id
    @GeneratedValue
    private Long id;
    @NonNull
    private String name;
    @NonNull
    private String role;
    private Long salary;
}