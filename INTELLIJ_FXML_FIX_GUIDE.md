# IntelliJ FXML Error Fix Guide

## How to Fix "FXCollections is not a valid type" and Similar FXML Errors

### Understanding the Error

When you see errors like:
```
Failed to load UserInterface
FXCollections is not a valid type
/path/to/your/file.fxml:57
```

This means the FXML file is trying to use a Java class that hasn't been properly imported in the FXML file.

---

## Step-by-Step Fix in IntelliJ

### Method 1: Using IntelliJ's FXML Editor (Recommended)

1. **Open the FXML File**
   - In IntelliJ, navigate to the file mentioned in the error (e.g., `search-panel.fxml`)
   - Path: `src/main/resources/com/example/teamse1csdchcw/fxml/search-panel.fxml`

2. **Switch to SceneBuilder or Text Mode**
   - At the bottom of the editor, you'll see tabs: "Text" and "SceneBuilder"
   - Click "Text" to edit the XML directly

3. **Add Missing Imports**
   - At the top of the FXML file, you'll see import statements like:
     ```xml
     <?import javafx.scene.control.*?>
     <?import javafx.scene.layout.*?>
     ```

4. **Add the Specific Import**
   - For `FXCollections` error, add:
     ```xml
     <?import javafx.collections.FXCollections?>
     <?import java.lang.String?>
     ```

   - The complete imports section should look like:
     ```xml
     <?xml version="1.0" encoding="UTF-8"?>

     <?import javafx.collections.FXCollections?>
     <?import javafx.geometry.Insets?>
     <?import javafx.scene.control.*?>
     <?import javafx.scene.layout.*?>
     <?import java.lang.String?>
     ```

5. **Save the File**
   - Press `Cmd+S` (Mac) or `Ctrl+S` (Windows/Linux)

6. **Rebuild the Project**
   - Go to **Build → Rebuild Project**
   - Or use shortcut: `Cmd+Shift+F9` (Mac) / `Ctrl+Shift+F9` (Windows/Linux)

7. **Run Again**
   - Click the green play button or use `Ctrl+R`

---

### Method 2: Using IntelliJ's Quick Fix

1. **Open the FXML File in Text Mode**

2. **Click on the Error Location**
   - IntelliJ usually highlights errors with red underlines
   - Click on the class name that's not imported (e.g., `FXCollections`)

3. **Use Quick Fix**
   - Press `Alt+Enter` (Windows/Linux) or `Option+Enter` (Mac)
   - Select "Import class" from the popup menu
   - IntelliJ will automatically add the import statement

---

### Method 3: Manual Edit (What You Just Learned)

1. **Identify the Missing Class**
   - Error says: "FXCollections is not a valid type"
   - This means we need to import `javafx.collections.FXCollections`

2. **Find the Full Class Path**
   - In IntelliJ, press `Cmd+N` (Mac) or `Ctrl+N` (Windows/Linux)
   - Type "FXCollections"
   - You'll see: `javafx.collections.FXCollections`

3. **Add the Import**
   - Add `<?import javafx.collections.FXCollections?>` at the top

---

## Common FXML Import Issues

### 1. FXCollections Not Found
**Error:** `FXCollections is not a valid type`

**Fix:**
```xml
<?import javafx.collections.FXCollections?>
<?import java.lang.String?>
```

### 2. ObservableList Not Found
**Error:** `ObservableList is not a valid type`

**Fix:**
```xml
<?import javafx.collections.ObservableList?>
```

### 3. Custom Controller Class Not Found
**Error:** `Cannot find class com.example.MyController`

**Fix:**
- Make sure the controller class exists
- Check the `fx:controller` attribute matches the full class path
- Rebuild the project

### 4. CSS File Not Found
**Error:** `Resource not found: @../css/style.css`

**Fix:**
- Check the CSS file exists in the correct location
- Paths in FXML are relative to the FXML file location
- Use `@` prefix for resources in the same package structure

---

## Understanding FXML Imports

### Wildcard vs Specific Imports

**Wildcard Import:**
```xml
<?import javafx.scene.control.*?>
```
- Imports all classes from `javafx.scene.control` package
- Does NOT include subpackages
- Convenient but can cause ambiguity

**Specific Import:**
```xml
<?import javafx.scene.control.Button?>
```
- Imports only the specific class
- More explicit and clear
- Required for classes in subpackages

### When to Use Each

**Use Wildcard (*) for:**
- Common packages you use many classes from
- Example: `javafx.scene.control.*`, `javafx.scene.layout.*`

**Use Specific Import for:**
- Utility classes like `FXCollections`
- Classes from subpackages
- Java standard library classes like `String`, `ArrayList`
- When you want explicit clarity

---

## Checking Your Imports in IntelliJ

### Enable FXML Validation

1. **Go to Preferences/Settings**
   - Mac: `Cmd+,`
   - Windows/Linux: `Ctrl+Alt+S`

2. **Navigate to:**
   - Editor → Inspections → JavaFX

3. **Enable:**
   - ✓ Missing JavaFX application class
   - ✓ Unresolved JavaFX resource
   - ✓ JavaFX unresolved FX ID reference

4. **Click Apply**

Now IntelliJ will highlight FXML errors as you type!

---

## Debugging FXML Loading Errors

### 1. Check the Console Output

When running with JavaFX, errors show the exact line:
```
/path/to/file.fxml:57
          ^^^ This is the line number
```

### 2. Use IntelliJ's Event Log

- Bottom right corner of IntelliJ
- Shows build errors and warnings
- Click on errors to jump to the location

### 3. Clean and Rebuild

Sometimes IntelliJ's cache gets stale:
1. **File → Invalidate Caches / Restart**
2. Select "Invalidate and Restart"
3. Wait for IntelliJ to restart and rebuild

### 4. Check Maven Dependencies

If classes are truly missing:
1. Open Maven panel (View → Tool Windows → Maven)
2. Click the reload button (circular arrows icon)
3. Expand Dependencies to verify JavaFX is present

---

## Common FXML Patterns in This Project

### ComboBox with Default Items

```xml
<ComboBox fx:id="maxResultsComboBox" prefWidth="80">
    <items>
        <FXCollections fx:factory="observableArrayList">
            <String fx:value="10"/>
            <String fx:value="25"/>
            <String fx:value="50"/>
            <String fx:value="100"/>
        </FXCollections>
    </items>
</ComboBox>
```

**Required Imports:**
```xml
<?import javafx.collections.FXCollections?>
<?import java.lang.String?>
```

### Including Other FXML Files

```xml
<fx:include source="search-panel.fxml"/>
```

**Note:** No additional imports needed, but the included file must be valid!

---

## Best Practices

### 1. Organize Imports Logically

```xml
<?xml version="1.0" encoding="UTF-8"?>

<!-- JavaFX Collections -->
<?import javafx.collections.FXCollections?>

<!-- JavaFX Geometry -->
<?import javafx.geometry.Insets?>

<!-- JavaFX Controls (wildcard for many classes) -->
<?import javafx.scene.control.*?>

<!-- JavaFX Layouts (wildcard for many classes) -->
<?import javafx.scene.layout.*?>

<!-- Java Standard Library -->
<?import java.lang.String?>
```

### 2. Use IntelliJ's FXML Preview

- Right side of FXML editor
- "Preview" tab shows the UI
- Errors appear in red

### 3. Keep Controllers and FXML in Sync

When FXML references:
```xml
<TextField fx:id="searchTextField"/>
```

Controller must have:
```java
@FXML
private TextField searchTextField;
```

### 4. Use Code Completion

In IntelliJ's FXML editor:
- Type `<` and IntelliJ suggests available components
- Type `fx:` and IntelliJ suggests FXML attributes
- Use `Ctrl+Space` for completion

---

## Troubleshooting Checklist

- [ ] FXML file has all necessary imports at the top
- [ ] Import paths are correct (check package names)
- [ ] Controller class exists and matches `fx:controller` attribute
- [ ] All `fx:id` references have matching `@FXML` fields in controller
- [ ] Project has been rebuilt (`Build → Rebuild Project`)
- [ ] Maven dependencies are loaded (check Maven panel)
- [ ] No typos in class names or package paths
- [ ] CSS and resource files exist at referenced paths

---

## Quick Reference: Common Imports

```xml
<!-- Collections -->
<?import javafx.collections.FXCollections?>
<?import javafx.collections.ObservableList?>

<!-- Controls -->
<?import javafx.scene.control.*?>

<!-- Layouts -->
<?import javafx.scene.layout.*?>

<!-- Geometry -->
<?import javafx.geometry.Insets?>
<?import javafx.geometry.Pos?>

<!-- Java Types -->
<?import java.lang.String?>
<?import java.lang.Integer?>
<?import java.lang.Double?>
<?import java.util.ArrayList?>

<!-- Events -->
<?import javafx.event.ActionEvent?>
```

---

## Next Steps

1. **Learn FXML Basics**
   - Oracle's JavaFX FXML documentation
   - JavaFX Scene Builder for visual editing

2. **Practice**
   - Create a simple FXML file
   - Add components
   - Connect to a controller

3. **Explore This Project**
   - Look at `main-view.fxml` - the main application window
   - Look at `search-panel.fxml` - the search interface
   - Look at `results-table.fxml` - the results display

4. **Use Scene Builder** (Optional)
   - Download from Gluon
   - Visual FXML editor
   - Generates FXML code for you

---

## Getting Help

When you encounter an FXML error:

1. **Read the error message carefully**
   - Notes the exact file and line number
   - Identifies the missing class

2. **Google the class name + "javafx"**
   - Find the correct package
   - Check JavaFX documentation

3. **Check IntelliJ's suggestions**
   - Red underlines often have quick fixes
   - `Alt+Enter` / `Option+Enter` for suggestions

4. **Ask on Stack Overflow**
   - Include the error message
   - Include the relevant FXML snippet
   - Mention you're using JavaFX 21

---

Remember: FXML errors are usually simple import issues. Once you understand the pattern, they're quick to fix!
