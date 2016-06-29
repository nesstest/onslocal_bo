**Local.ONS Alpha Back Office User Guide**

**Introduction**

The back office application created for Local.ONS is actually intended
to work for all multidimensional data, whether it be geographic, time
series or both.

The design is deliberately simple and is influenced by what is known to
be good and not good in the NeSS and WDA back offices.

The application is written in Scala and Java using the Play Framework
2.5. It has a relational database implemented via PostgreSQL 9.5,
accessed using JPA for rapid development. Some code was lifted from WDA
but not that much was directly usable (and obviously not any of the
Talend ETL scripts).

The workflow is simplistic (see status screen) and there is no delete or
replace functionality. Publishing has not been implemented but Play does
allow the controllers to be exposed as a web service, so Florence could
make requests over HTTP.

The code is stored in GitHub and the application is deployed to AWS
(currently a manual process). The access rules in AWS only allow access
from the ONS Network or WiFi in Newport or Titchfield.

![alt text](https://github.com/nesstest/onslocal_bo/tree/master/public/images/image1.png "Screen shot"){width="6.260416666666667in"
height="4.927083333333333in"}

**Step 1 - Load To Staging**

The back office database is the same as the front office with the
exception of a staging area for datasets. Whilst in theory it is
possible to populate the actual tables used by the front end in a single
pass from the input CSV file, it is much more practical to initially
load the data into tables that match the input file’s structure. All the
data is then easily accessible via SQL in the next step (load to
target).

The input file must be in the current WDA input format as produced by
DataBaker (see Appendix A). In future we would expect this format to be
simplified and tidied up (the WDA one has a lot of baggage).

![alt text](https://github.com/nesstest/onslocal_bo/tree/master/public/images/image2.png "Screen shot"){width="6.260416666666667in" height="3.6875in"}

The Dataset ID is up to 10 digits alphanumeric (no spaces or special
characters allowed). This is the Data Resource ID (equivalent to dataset
family in NeSS) on the database which allows there to be one or many
DimensionalDataSet (dataset instance) entries associated with it. In the
Alpha you can only have one-to-one so we simply refer to the 10 digit
code as “Dataset Id” throughout the system.

The Dataset Title should be a meaningful short description.

Clicking the browse button launches a file selection dialogue. The
chosen file should be a WDA-format CSV file and it must be below the
maximum file size specified in AWS (100MB).

On pressing the load button the input file is uploaded to AWS then the
staging records created. Validation errors will be reported and also
viewable on the status screen and logs.

**Step 2 - Load To Target**

This is a complex process that transforms the staging data to real
records. It has to populate a number of tables:

Geographic Areas – as the input file has only the external code we’ve
preloaded a selection of areas from the FO database, so the system does
not attempt to create new ones (error if area not present).

Time Periods – in this case we have not precreated any so we don’t know
if the incoming ones will be new or already existing, so have to cope
with both.

Note that there is a check made for there being only one area or only
one time at the start. Geography and Time values are used to populate
the Population table.

Categories – each staging observation record has 1 or more categories
associated with it (depending on the number of topic dimensions). Again
the system has to create new ones if necessary. It does this blindly so
you can end up with slightly different versions of the same thing.

Variables – each combination of categories must be checked and a new
variable entry created if necessary.

Finally, armed with all the above for a single record, the observation
can be saved.

This is not an efficient way to load the data and performance is not
good. To make this run fast enough for large datasets a lot of work is
required.

![alt text](https://github.com/nesstest/onslocal_bo/tree/master/public/images/image3.png "Screen shot"){width="6.260416666666667in" height="2.25in"}

The only input is the dataset id (which you have to remember or get from
the Status screen).

Errors are reported to the logs and usually also the status screen (some
database errors which result in a rollback are not captured – on snag
list).

**Step 3 - Edit Attributes**

In the WDA back office there is a lot of customisation and tweaking that
you can do to the datasets before document generation and publication.
For the Alpha we decided to only implement a single function, this being
the specification of what goes in the row and what in the column when
displaying the table in the FO.

Missing from here is measurement units, statistical units and
multipliers which are usually not specified on the WDA input files, so
need to be set by the BO user. The Alpha takes whatever is given in the
input file, and if this is blank you get Persons and Count by default.

![alt text](https://github.com/nesstest/onslocal_bo/tree/master/public/images/image4.png "Screen shot"){width="6.260416666666667in"
height="2.9791666666666665in"}

Currently all the “concepts” (high level classifications such as age and
sex) in the system are offered plus Geographic Area and Time Period.
Should really filter the concepts to those used on the current dataset
(on the snag list).

**Step 4 – Load Metadata**

The database holds metadata fields for a dataset that match the DCAT
specification (see <http://guidance.data.gov.uk/harvesting.html#dcat>).
This metadata will be provided separately via a new tool called
MetaMaker. There are a number of ways this data could be transferred and
ingested (such as an exposed service). For simplicity in the Alpha we
are using copy and paste into a text box.

![alt text](https://github.com/nesstest/onslocal_bo/tree/master/public/images/image5.png "Screen shot"){width="6.270833333333333in"
height="4.333333333333333in"}

The user has to enter the dataset ID then paste the metadata into the
box and click the import button. The input is then validated and saved
to the database.

The database supports a two-way process, so an enhanced version of this
screen could also allow you to change one or more of the individual
fields and regenerate the JSON (held in a CLOB on the database) as well
as updating the individual fields from the JSON as it does now.

Note that the “modified” date gets automatically changed to now at the
end as the system does this after every step.

**Step 5 – Generate CSV**

The next step is to generate a CSV file from the database for the entire
dataset.

This, plus a metadata JSON file, forms the download data package
uploaded to the cloud as part of the publish process.

In the Alpha it is the main way of checking the data has loaded
correctly (the screen shot below looks suspicious to me!)

The logic used is currently the simplest possible but could be a lot
more sophisticated:

The rows are area by time (times nested within areas) so this works for
all types of data. The column is the variable (not broken up into nested
categories).

Time Periods are sorted alphabetically rather than chronologically (snag
list item to fix this).

![alt text](https://github.com/nesstest/onslocal_bo/tree/master/public/images/image6.png "Screen shot"){width="6.260416666666667in" height="5.0in"}

For simplicity, the generated CSV is held in the logfile area on the
server and streamed straight to the user who can either open it (e.g. in
Excel) or save to a local file.

**Step 6 – Publish Dataset**

Publishing is out of scope for the Alpha but we hope to fake it but
pointing the FO API to the BO database, and check that the datasets
work.

![alt text](https://github.com/nesstest/onslocal_bo/tree/master/public/images/image7.png "Screen shot"){width="6.270833333333333in"
height="4.927083333333333in"}

![alt text](https://github.com/nesstest/onslocal_bo/tree/master/public/images/image8.png "Screen shot"){width="6.260416666666667in"
height="2.6458333333333335in"}

**Utility – Status Screen**

All the datasets in the system are listed here. Dataset ID and Last
Updated are self-explanatory…

*List of Statuses*

1-Staging-OK 1-Staging-Failed

2-Target-OK 2-Target-Failed

3-Attributes-OK 3-Attributes-Failed

4-Metadata-OK 4-Metadata-Failed

5-Generate-OK 5-Generate-Failed

6-Publish-OK 6-Publish-Failed

*Num Obs*

This value is saved to the dataset when load to target is successful. It
is the actual number loaded which may not be all the staging records
(during the initial testing there is a limit of 1000 cells).

*Error Messages*

Can be validation errors (system finds a problem with the data) or load
errors (unexpected runtime failures)

![alt text](https://github.com/nesstest/onslocal_bo/tree/master/public/images/image9.png "Screen shot"){width="6.260416666666667in"
height="2.6145833333333335in"}

There is currently no user functionality such as sorting or filtering.
This is OK for the Alpha but would be essential in a later version.

**Utility – View Log**

A useful facility to be able to browse the application logs, both for
informational messages when things have worked and error messages when
failures occur.

![alt text](https://github.com/nesstest/onslocal_bo/tree/master/public/images/image10.png "Screen shot"){width="6.260416666666667in" height="4.71875in"}

Currently, the log grows continuously until the application is restarted
or redeployed, there is no option to show (say) only today’s logs or a
particular level of logging (e.g. errors only, info only).

**Appendix A – WDA Input Format**

A Column 0 Observation value (number) --observation

B Column 1 Data marking String --observation

C Column 2 Statistical Unit Eng value --attribute

D Column 3 Statistical Unit welsh value --attribute

E Column 4 Meas type eng --attribute

F Column 5 Meas type welsh --attribute

G Column 6--&gt;obs type code --observation

H Column 7--&gt;empty --ignore

I Column 8--&gt;obs type val --observation

J Column 9--&gt;unit mult scalar(english value) --attribute

K Column 10--&gt;unit of meas eng --attribute

L Column 11--&gt;unit of meas welsh --attribute

M Column 12--&gt;confidentiality --observation

N Column 13--&gt;empty --ignore

O Column 14--&gt;geog --geog\_item

P Column 15--&gt;empt --ignore

Q Column 16--&gt;empt --ignore

R Column 17--&gt;Time Dim Item ID --dimension item (CL\_TIME)

S Column 18--&gt;Time Dim Item Label Eng --dimension item

T Column 19--&gt;Time Dim Item Label Welsh --dimension item

U Column 20--&gt;time type --classification item type (Year, Month,
Quarter)

V Column 21--&gt;emp --ignore

W Column 22--&gt;Statistical Population ID -- segment

X Column 23--&gt;Statistical Population Label Eng -- segment

Y Column 24--&gt;Statistical Population Label welsh -- segment

Z Column 25--&gt;CDID --dim\_item\_set

AA Column 26--&gt;CDID Description --dim\_item\_set

AB Column 27--&gt;empt --ignore

AC Column 28--&gt;empt --ignore

AD Column 29--&gt;empt --ignore

AE Column 30--&gt;empt --ignore

AF Column 31--&gt;empt --ignore

AG Column 32--&gt;empt --ignore

AH Column 33--&gt;empt --ignore

AI Column 34--&gt;empt --ignore

AJ Column 35--&gt;Dim ID 1

AK Column 36--&gt;Dimension Label Eng 1

AL Column 37--&gt;Dimension Label Welsh 1

AM Column 38--&gt;Dim Item ID 1

AN Column 39--&gt;Dimension Item Label Eng 1

AO Column 40--&gt;Dimension Item Label Cym 1

AP Column 41--&gt;Is Total 1

AQ Column 42--&gt;Is Subtotal 1

// 35-42 cols will be repeated if there are more dimensions

Written by: Richard Smith
