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
replace functionality. Publishing has not been implemented but can be simulated by changing the database connection the API the web front end uses to point at the BO schema. Note also that Play does allow controller actions to be exposed as a web service, e.g. /generate/POPTEST1 returns a CSV file. This is a way the new website's publication system could make requests over HTTP.

The code is stored in GitHub and the application is deployed to AWS
(currently a manual process). The access rules in AWS only allow access
from the ONS Network or WiFi in Newport or Titchfield.

![alt text](https://raw.githubusercontent.com/nesstest/onslocal_bo/master/public/images/image1.png "Screen shot")

**Step 1 - Load To Staging**

The back office database is the same as the front office with the
exception of a staging area for datasets. Whilst in theory it is
possible to populate the actual tables used by the front end in a single
pass from the input CSV file, it is much more practical to initially
load the data into tables that match the input file's structure. All the
data is then easily accessible via SQL in the next step (load to
target).

The input file must be in the current WDA input format as produced by
DataBaker. In future we expect this format to be
simplified and tidied up (the WDA one has a lot of baggage).

![alt text](https://raw.githubusercontent.com/nesstest/onslocal_bo/master/public/images/image2.png "Screen shot")

The Dataset ID is up to 10 digits alphanumeric (no spaces or special
characters allowed). This is the Data Resource ID (equivalent to dataset
family in NeSS) on the database which allows there to be one or many
DimensionalDataSet (dataset instance) entries associated with it. In the
Alpha you can only have one-to-one so we simply refer to the 10 digit
code as 'Dataset Id' throughout the system.

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

Geographic Areas - as the input file has only the external code we've
preloaded a selection of areas from the FO database, so the system does
not attempt to create new ones (error if area not present).

Time Periods - in this case we have not precreated any so we don't know
if the incoming ones will be new or already existing, so have to cope
with both (a helper class works out the start and end dates).

Note that there is a check made for there being only one area or only
one time at the start. Geography and Time values are used to populate
the Population table.

Categories - each staging observation record has 1 or more categories
associated with it (depending on the number of topic dimensions). Again
the system has to create new ones if necessary. It does this blindly so
you can end up with slightly different versions of the same thing.

Variables - each combination of categories must be checked and a new
variable entry created if necessary.

Finally, armed with all the above for a single record, the observation
can be saved.

This is not an efficient way to load the data and performance is not
good (about 2,000 cells per minute). This is OK for the Alpha but a more efficient solution will be needed for the Beta which must cope with datasets with millions of cells.

![alt text](https://raw.githubusercontent.com/nesstest/onslocal_bo/master/public/images/image3.png "Screen shot")

The only input is the dataset id which you choose from a dropdown list of those with the status 1-Staging-OK.

Errors are reported to the logs and usually also the status screen (some
database errors which result in a rollback are not captured - to be fixed).

**Step 3 - Edit Attributes**

In the WDA back office there is a lot of customisation and tweaking that
you can do to the datasets before document generation and publication.
For the Alpha we decided to only implement two functions: 
1) The specification of what goes in the row and what in the column when
displaying the table in the FO.
2) Associating the dataset with one taxonomy node (needed for search in FO).

Missing from here is measurement units, statistical units and
multipliers which are usually not specified on the WDA input files, so
need to be set by the BO user. The Alpha takes whatever is given in the
input file, and if this is blank you get Persons and Count by default.

![alt text](https://raw.githubusercontent.com/nesstest/onslocal_bo/master/public/images/image4.png "Screen shot")

For the row and column dimensions, the user is presented with the options "Geographic Area", "Time Period" and all the concepts (equivalent to topic dimensions) used on the current dataset. For the taxonomy, a drop-down is driven by the taxonomy table. For all three lists the current setting (if any) is selected on page load.

**Step 4 - Load Metadata**

The database holds metadata fields for a dataset that match the DCAT
specification (see <http://guidance.data.gov.uk/harvesting.html#dcat>).
This metadata will be provided separately via a new tool called
MetaMaker. There are a number of ways this data could be transferred and
ingested (such as an exposed service). For simplicity in the Alpha we
are using copy and paste into a text box.

![alt text](https://raw.githubusercontent.com/nesstest/onslocal_bo/master/public/images/image5.png "Screen shot")

The user has to select the dataset ID from a list of those with the correct status 
then paste the metadata into the
box and click the import button. The input is then validated and saved
to the database.

The database supports a two-way process, so an enhanced version of this
screen could also allow you to change one or more of the individual
fields and regenerate the JSON (held in a CLOB on the database) as well
as updating the individual fields from the JSON as it does now.

Note that the 'modified' date gets automatically changed to now at the
end as the system does this after every step.

**Step 5 - Generate CSV**

The next step is to generate a CSV file from the database for the entire
dataset.

This, plus a metadata JSON file, forms the download data package
uploaded to the cloud as part of the publish process.

In the Alpha it is the main way of checking the data has loaded
correctly.

The logic used is currently the simplest possible but could be a lot
more sophisticated:

The rows are area by time (times nested within areas) so this works for
all types of data. The column is the variable (not broken up into nested
categories).

Time Periods are now sorted chronologically, but there is still a problem with sparsity. Datasets with zeroes or missing values work but those with no cell at all for certain combinations result in an incorrect output file.

As a by-product of the generation process, the presentation table is update to make the downloads work on the FO, though these are in fact service calls to the BO to generate the CSV on-they-fly. This is necessary because the system does not permanently store the generated files. 

![alt text](https://raw.githubusercontent.com/nesstest/onslocal_bo/master/public/images/image6.png "Screen shot")

For simplicity, the generated CSV is held in the logfile area on the
server and streamed straight to the user who can either open it (e.g. in
Excel) or save to a local file.

**Step 6 - Publish Dataset**

Publishing is out of scope for the Alpha but we have faked it by pointing the FO API to the BO database, and (following a few tweaks) the datasets do work.

![alt text](https://raw.githubusercontent.com/nesstest/onslocal_bo/master/public/images/image7.png "Screen shot")

![alt text](https://raw.githubusercontent.com/nesstest/onslocal_bo/master/public/images/image8.png "Screen shot")

**Utility - Status Screen**

All the datasets in the system are listed here. Dataset ID and Last
Updated are self-explanatory.

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
(e.g. during the initial testing there was a cell limit in operation).

*Error Messages*

Can be validation errors (system finds a problem with the data) or load
errors (unexpected runtime failures)

![alt text](https://raw.githubusercontent.com/nesstest/onslocal_bo/master/public/images/image9.png "Screen shot")

There is currently no user functionality such as sorting or filtering.
This is OK for the Alpha but would be essential in a later version.

**Utility - View Log**

A useful facility to be able to browse the application logs, both for
informational messages when things have worked and error messages when
failures occur.

![alt text](https://raw.githubusercontent.com/nesstest/onslocal_bo/master/public/images/image10.png "Screen shot")

Currently, the log grows continuously until the application is restarted
or redeployed, there is no option to show (say) only today's logs or a
particular level of logging (e.g. errors only, info only).

Logs are now set to UK time (the screen shot was taken when they showed the time in Western USA where the AWS host server resides).


