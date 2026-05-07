@echo off
cd /d "%~dp0"
REM Compile with all source files including DAO and model classes
javac --module-path "C:\Program Files\javafx-sdk-21.0.10\lib" --add-modules javafx.controls,javafx.graphics,javafx.fxml -cp "lib\*" -d bin ^
    src\module-info.java ^
    src\application\DatabaseConnection.java ^
    src\application\User.java ^
    src\application\Student.java ^
    src\application\Teacher.java ^
    src\application\Subject.java ^
    src\application\SchoolClass.java ^
    src\application\Level.java ^
    src\application\Grade.java ^
    src\application\AnneeScolaire.java ^
    src\application\StudentDAO.java ^
    src\application\TeacherDAO.java ^
    src\application\ClassDAO.java ^
    src\application\GradeDAO.java ^
    src\application\SubjectDAO.java ^
    src\application\LevelDAO.java ^
    src\application\AnneeScolaireDAO.java ^
    src\application\Authentification.java ^
    src\application\MenuGeneral.java ^
    src\application\AdminPanelStudents.java ^
    src\application\AdminPanelClasses.java ^
    src\application\AdminPanelLevels.java ^
    src\application\AdminPanelTeachers.java ^
    src\application\AdminPanelSubjects.java ^
    src\application\AdminPanelAssignments.java ^
    src\application\AdminPanelStudentBoard.java ^
    src\application\AdminPanelBulletins.java ^
    src\application\AdminPanelAnneeScolaire.java ^
    src\application\TeacherPanelClasses.java ^
    src\application\TeacherPanelGrades.java ^
    src\application\TeacherPanelViewGrades.java ^
    src\application\Main.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)
REM Run the application with SQLite JDBC in classpath
java --module-path "C:\Program Files\javafx-sdk-21.0.10\lib;bin" --add-modules javafx.controls,javafx.graphics,javafx.fxml -cp "lib\*" -m TP_JavaFX/application.Main
pause
