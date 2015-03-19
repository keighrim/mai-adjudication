## Multi-document Adjudication Interface

### Introducing MAI
MAI (Multi-document Adjudication Interface) is an adjudication tool originally created by [Amber Stubbs] (http://amberstubbs.net) for Brandeis University for use in her dissertation research. It is a lightweight program written in Java, with a MySQLite database back end (SQLiteJDBC driver created by [David Crawshaw](http://www.zentus.com/sqlitejdbc/)).

MAI takes as input any standoff annotated documents (for best results, the files output by MAE should be used), and it allows for easy adjudication of extent tags, link tags, and non-consuming tags.

### Download Current version
Currently Keigh Rim (krim@brandeis.edu) is working on maintenance jobs and updating new features to MAI. The current version of MAI is 0.10.0. You can download most recent version by cloning this repository.

    git clone https://github.com/keighrim/mai-adjudication

### Requirements
Current version of MAI is written in JAVA. Thus, to run MAI on your local system, you need JAVA later than 6. Use JAR to run MAI
    
    > java -jar mai.jar

### Changes
See [CHANGELOG.md] (https://github.com/keighrim/mai-adjudication/blob/master/CHANGELOG.md)

### License
MAI is free software: you can redistribute it and/or modify it under the terms of the [GNU General Public License](http://www.gnu.org/licenses/gpl.html) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

#### See also
The Multi-purpose Annotation Environment (MAE) is a companion program to MAI that allows you to manage annotation tasks and annotate texts. MAE is available for download at  https://github.com/keighrim/mae-annotation

For a detailed user guide, please consult Amber Stubbs' book [Natural Language Annotation for Machine Learning](http://www.amazon.com/Natural-Language-Annotation-Machine-Learning/dp/1449306667/). (Please be advised than the guide written for 0.7.1)

You can also visit old code site hosted on Google Code https://code.google.com/p/mai-adjudication/
