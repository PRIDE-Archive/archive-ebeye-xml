package uk.ac.ebi.pride.archive.ebeye;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.ac.ebi.pride.archive.repo.project.*;
import uk.ac.ebi.pride.archive.repo.user.User;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * GenerateEBeyeXML object.
 *
 * Generates EB-eye search XML to a given output directory based upon a PRIDE Archive project
 * supplied as a Project and Submission.
 *
 * @author  Tobias Ternent  tobias@ebi.ac.uk
 * @author  Yasset Perez-Riverol ypriverol@gmail.com
 * @version 1.0
 * @since   2015-02-10
 */
public class GenerateEBeyeXML {

    private static final Logger logger = LoggerFactory.getLogger(GenerateEBeyeXML.class);

    private static final String NOT_AVAILABLE      = "Not available";

    private static final String OMICS_TYPE         = "Proteomics";

    private static final String PRIDE_URL          = "http://www.ebi.ac.uk/pride/archive/projects/";

    private static final String DEFAULT_EXPERIMENT_TYPE  = "Mass Spectrometry";

    private Project project;

    private File outputDirectory;

    private Submission submission;

    private HashMap<String, String> proteins;

    private boolean fromPride;

    /**
     * Constructor, without parameters.
     */
    public GenerateEBeyeXML() {
    }

    /**
     * Constructor.
     *
     * @param project   (required) public project to be used for generating the EB-eye XML.
     * @param submission    (required) public project submission summary to be used for generating the EB-eye XML.
     * @param outputDirectory   (required) target output directory.
     */
    public GenerateEBeyeXML(Project project, Submission submission, File outputDirectory, HashMap<String, String> proteins) {
        this.project = project;
        this.submission = submission;
        this.outputDirectory = outputDirectory;
        this.proteins = proteins;
        this.fromPride = false;
    }

    public GenerateEBeyeXML(Project project, Submission submission, File outputDirectory, HashMap<String, String> proteins, boolean fromPride) {
        this.project = project;
        this.submission = submission;
        this.outputDirectory = outputDirectory;
        this.proteins = proteins;
        this.fromPride = fromPride;
    }

    /**
     * Performs the EB-eye generation of a defined public project, submission summary, and output directory.
     * @throws Exception
     */
    public void generate() throws Exception {
        if (project==null || submission==null || outputDirectory==null) {
            logger.error("The project, submission, and output directory all needs to be set before generating EB-eye XML.");
        }
        if (!project.isPublicProject()) {
            logger.error("Project " + project.getAccession() + " is still private, not generating EB-eye XML.");
        } else {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            //Add database Name Node

            Element database = document.createElement("database");
            document.appendChild(database);

            //Add the name of the database
            Element name = document.createElement("name");
            name.appendChild(document.createTextNode("PRIDE Archive"));
            database.appendChild(name);

            //Add the description of the database
            Element description = document.createElement("description");
            description.appendChild(document.createTextNode(""));
            database.appendChild(description);

            //Database release
            Element release = document.createElement("release");
            release.appendChild(document.createTextNode("3"));
            database.appendChild(release);

            //Release date (This release date is related whit the day where the data was generated)
            Element releaseDate = document.createElement("release_date");
            releaseDate.appendChild(document.createTextNode(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));
            database.appendChild(releaseDate);

            Element entryCount = document.createElement("entry_count");
            entryCount.appendChild(document.createTextNode("1"));
            database.appendChild(entryCount);

            //Start to index the entries of the project
            Element entries = document.createElement("entries");
            database.appendChild(entries);

            //The project entry to be fill in the document
            Element entry = document.createElement("entry");
            entry.setAttribute("id", project.getAccession());

            Element projectName = document.createElement("name");
            projectName.appendChild(document.createTextNode(project.getTitle()));
            entry.appendChild(projectName);

            String projDescription = project.getTitle();
            if (project.getProjectDescription()!=null && !project.getProjectDescription().isEmpty())
                projDescription = project.getProjectDescription();

            Element projectTitle = document.createElement("description");
            projectTitle.appendChild(document.createTextNode(projDescription));
            entry.appendChild(projectTitle);


            /**
             * Add all cross references to other databases such as TAXONOMY, UNIPROT OR ENSEMBL
             */

            Element crossReferences = document.createElement("cross_references");
            entry.appendChild(crossReferences);

            if (submission.getProjectMetaData().getSpecies()!=null && submission.getProjectMetaData().getSpecies().size()>0) {
                for (CvParam species : submission.getProjectMetaData().getSpecies()) {
                    Element refSpecies = document.createElement("ref");
                    refSpecies.setAttribute("dbkey", species.getAccession());
                    refSpecies.setAttribute("dbname", "TAXONOMY");
                    crossReferences.appendChild(refSpecies);
                }
            }

            if (project.getReferences()!=null && project.getReferences().size()>0) {
                for (Reference reference : project.getReferences()) {
                    Element refPubMedID = document.createElement("ref");
                    refPubMedID.setAttribute("dbkey", Integer.toString(reference.getPubmedId()));
                    refPubMedID.setAttribute("dbname", "pubmed");
                    crossReferences.appendChild(refPubMedID);
                }
            }

            if (proteins!=null && !proteins.isEmpty()) {
                for (String protein : proteins.keySet()) {
                    Element refProtein = document.createElement("ref");
                    refProtein.setAttribute("dbkey", protein);
                    refProtein.setAttribute("dbname", proteins.get(protein));
                    crossReferences.appendChild(refProtein);
                }
            }

            Element dates = document.createElement("dates");
            entry.appendChild(dates);

            Element dateSubmitted = document.createElement("date");
            dateSubmitted.setAttribute("value", new SimpleDateFormat("yyyy-MM-dd").format(project.getSubmissionDate()));
            dateSubmitted.setAttribute("type", "submission");
            dates.appendChild(dateSubmitted);

            Element datePublished = document.createElement("date");
            datePublished.setAttribute("value", new SimpleDateFormat("yyyy-MM-dd").format(project.getPublicationDate()));
            datePublished.setAttribute("type", "publication");
            dates.appendChild(datePublished);


            /**
             * Add additional Fields for DDI project to be able to find the projects. Specially additional metadata
             * such as omics field, ptms, study type, data protocol sample protocol, etc.
             */

            Element additionalFields = document.createElement("additional_fields");
            entry.appendChild(additionalFields);


            // Add the omics type
            Element omicsType = document.createElement("field");
            omicsType.setAttribute("name", "omics_type");
            omicsType.appendChild(document.createTextNode(OMICS_TYPE));
            additionalFields.appendChild(omicsType);


            //Add the Sample Processing Protocol
            if (project.getSubmissionDate()!=null) {
                Element submissionDate = document.createElement("field");
                submissionDate.setAttribute("name", "submission_date");
                submissionDate.appendChild(document.createTextNode(new SimpleDateFormat("yyyy-MM-dd").format(project.getSubmissionDate())));
                additionalFields.appendChild(submissionDate);
            }

            //Full dataset Repository
            Element full_dataset_link = document.createElement("field");
            full_dataset_link.setAttribute("name", "full_dataset_link");
            full_dataset_link.appendChild(document.createTextNode(PRIDE_URL + project.getAccession()));
            additionalFields.appendChild(full_dataset_link);

            //Add the domain source
            Element respository = document.createElement("field");
            respository.setAttribute("name", "domain_source");
            respository.appendChild(document.createTextNode("pride"));
            additionalFields.appendChild(respository);


            //Add the Sample Processing Protocol
            if (project.getPublicationDate()!=null) {
                Element publicationDate = document.createElement("field");
                publicationDate.setAttribute("name", "publication_date");
                publicationDate.appendChild(document.createTextNode(new SimpleDateFormat("yyyy-MM-dd").format(project.getPublicationDate())));
                additionalFields.appendChild(publicationDate);
            }

            //Publication Date
            if (project.getSampleProcessingProtocol()!=null && !project.getSampleProcessingProtocol().isEmpty()) {
                Element sampleProcProt = document.createElement("field");
                sampleProcProt.setAttribute("name", "sample_protocol");
                sampleProcProt.appendChild(document.createTextNode(project.getSampleProcessingProtocol()));
                additionalFields.appendChild(sampleProcProt);
            }

            //Add Data Processing Protocol
            if (project.getDataProcessingProtocol()!=null && !project.getDataProcessingProtocol().isEmpty()) {
                Element dataProcProt = document.createElement("field");
                dataProcProt.setAttribute("name", "data_protocol");
                dataProcProt.appendChild(document.createTextNode(project.getDataProcessingProtocol()));
                additionalFields.appendChild(dataProcProt);
            }

            //Add Instrument information
            if (submission.getProjectMetaData().getInstruments()!=null && submission.getProjectMetaData().getInstruments().size()>0) {
                for (CvParam instrument : submission.getProjectMetaData().getInstruments()) {
                    Element fieldInstruemnt = document.createElement("field");
                    fieldInstruemnt.setAttribute("name", "instrument_platform");
                    fieldInstruemnt.appendChild(document.createTextNode(instrument.getName()));
                    additionalFields.appendChild(fieldInstruemnt);
                }
            } else {
                Element fieldInstruemnt = document.createElement("field");
                fieldInstruemnt.setAttribute("name", "instrument_platform");
                fieldInstruemnt.appendChild(document.createTextNode(NOT_AVAILABLE));
                additionalFields.appendChild(fieldInstruemnt);
            }

            //Add information about the species
            if (submission.getProjectMetaData().getSpecies()!=null && submission.getProjectMetaData().getSpecies().size()>0) {
                for (CvParam species : submission.getProjectMetaData().getSpecies()) {
                    Element refSpecies = document.createElement("field");
                    refSpecies.setAttribute("name", "species");
                    refSpecies.appendChild(document.createTextNode(species.getName()));
                    additionalFields.appendChild(refSpecies);
                }
            } else {
                Element refSpecies = document.createElement("field");
                refSpecies.setAttribute("name", "species");
                refSpecies.appendChild(document.createTextNode(NOT_AVAILABLE));
                additionalFields.appendChild(refSpecies);
            }

            //Add information about the Cell Type
            if (submission.getProjectMetaData().getCellTypes()!=null && submission.getProjectMetaData().getCellTypes().size()>0) {
                for (CvParam cellType : submission.getProjectMetaData().getCellTypes()) {
                    Element refCellType = document.createElement("field");
                    refCellType.setAttribute("name", "cell_type");
                    refCellType.appendChild(document.createTextNode(cellType.getName()));
                    additionalFields.appendChild(refCellType);
                }
            } else {
                Element refCellType = document.createElement("field");
                refCellType.setAttribute("name", "cell_type");
                refCellType.appendChild(document.createTextNode(NOT_AVAILABLE));
                additionalFields.appendChild(refCellType);
            }

            //Add disease information
            if (submission.getProjectMetaData().getDiseases()!=null && submission.getProjectMetaData().getDiseases().size()>0) {
                for (CvParam disease : submission.getProjectMetaData().getDiseases()) {
                    Element refDisease = document.createElement("field");
                    refDisease.setAttribute("name", "disease");
                    refDisease.appendChild(document.createTextNode(disease.getName()));
                    additionalFields.appendChild(refDisease);
                }
            } else {
                Element refDisease = document.createElement("field");
                refDisease.setAttribute("name", "disease");
                refDisease.appendChild(document.createTextNode(NOT_AVAILABLE));
                additionalFields.appendChild(refDisease);
            }

            //Tissue information
            if (submission.getProjectMetaData().getTissues()!=null && submission.getProjectMetaData().getTissues().size()>0) {
                for (CvParam tissue : submission.getProjectMetaData().getTissues()) {
                    Element fieldTissue = document.createElement("field");
                    fieldTissue.setAttribute("name", "tissue");
                    fieldTissue.appendChild(document.createTextNode(tissue.getName()));
                    additionalFields.appendChild(fieldTissue);
                }
            } else {
                Element fieldTissue = document.createElement("field");
                fieldTissue.setAttribute("name", "tissue");
                fieldTissue.appendChild(document.createTextNode(NOT_AVAILABLE));
                additionalFields.appendChild(fieldTissue);
            }

            //Add PTMs information
            if (project.getPtms()!=null && project.getPtms().size()>0) {
                for (ProjectPTM ptmName : project.getPtms()) {
                    Element modification = document.createElement("field");
                    modification.setAttribute("name", "modification");
                    modification.appendChild(document.createTextNode(ptmName.getName()));
                    additionalFields.appendChild(modification);
                }
            } else {
                Element modification = document.createElement("field");
                modification.setAttribute("name", "modification");
                modification.appendChild(document.createTextNode(NOT_AVAILABLE));
                additionalFields.appendChild(modification);
            }

            //Add information about experiment type
            if (project.getExperimentTypes()!=null && project.getExperimentTypes().size()>0) {
                for (ProjectExperimentType expType : project.getExperimentTypes()) {
                    Element refExpType = document.createElement("field");
                    refExpType.setAttribute("name", "technology_type");
                    refExpType.appendChild(document.createTextNode(expType.getName()));
                    additionalFields.appendChild(refExpType);
                }
            }

            Element refExpType = document.createElement("field");
            refExpType.setAttribute("name", "technology_type");
            refExpType.appendChild(document.createTextNode(DEFAULT_EXPERIMENT_TYPE));
            additionalFields.appendChild(refExpType);


            //Add curator tags and keywords
            if (project.getProjectTags()!=null && project.getProjectTags().size()>0) {
                for (ProjectTag projectTag : project.getProjectTags()) {
                    Element fieldProjTag = document.createElement("field");
                    fieldProjTag.setAttribute("name", "curator_keywords");
                    fieldProjTag.appendChild(document.createTextNode(projectTag.getTag()));
                    additionalFields.appendChild(fieldProjTag);
                }
            }

            if (project.getKeywords()!=null && !project.getKeywords().isEmpty()) {
                //Keywords should be a list of keywords splitted by comma
                String[] arrayKey = project.getKeywords().split(",");
                for(String key: arrayKey){
                    Element keywords = document.createElement("field");
                    keywords.setAttribute("name", "submitter_keywords");
                    keywords.appendChild(document.createTextNode(project.getKeywords()));
                    additionalFields.appendChild(keywords);
                }
            }

            //Specific to proteomics field the quantitation method
            if (project.getQuantificationMethods()!=null && project.getQuantificationMethods().size()>0) {
                for (ProjectQuantificationMethodCvParam quantMethod : project.getQuantificationMethods()) {
                    Element refQuantMethod = document.createElement("field");
                    refQuantMethod.setAttribute("name", "quantification_method");
                    refQuantMethod.appendChild(document.createTextNode(quantMethod.getName()));
                    additionalFields.appendChild(refQuantMethod);
                }
            } else {
                Element quantMethod = document.createElement("field");
                quantMethod.setAttribute("name", "quantification_method");
                quantMethod.appendChild(document.createTextNode(NOT_AVAILABLE));
                additionalFields.appendChild(quantMethod);
            }

            Element submissionType = document.createElement("field");
            submissionType.setAttribute("name", "submission_type");
            submissionType.appendChild(document.createTextNode(project.getSubmissionType().name()));
            additionalFields.appendChild(submissionType);

            if (project.getSoftware()!=null && project.getSoftware().size()>0) {
                for (ProjectSoftwareCvParam software : project.getSoftware()) {
                    Element refSoftware = document.createElement("field");
                    refSoftware.setAttribute("name", "software");
                    refSoftware.appendChild(document.createTextNode(software.getValue()));
                    additionalFields.appendChild(refSoftware);
                }
            } else {
                Element refSoftware = document.createElement("field");
                refSoftware.setAttribute("name", "software");
                refSoftware.appendChild(document.createTextNode(NOT_AVAILABLE));
                additionalFields.appendChild(refSoftware);
            }

            //Add publication related information
            if (project.getDoi()!=null && !project.getDoi().isEmpty()) {
                Element doi = document.createElement("field");
                doi.setAttribute("name", "doi");
                doi.appendChild(document.createTextNode(project.getDoi()));
                additionalFields.appendChild(doi);
            }

            //Add publication related information
            if (project.getReferences()!=null && project.getReferences().size()>0) {
                for (Reference reference : project.getReferences()) {
                    Element refPubMedLine = document.createElement("field");
                    refPubMedLine.setAttribute("name", "publication");
                    String referenceLine = "";
                    if (reference.getPubmedId()!=0) {
                        referenceLine = reference.getPubmedId() + "";
                    }
                    if (reference.getReferenceLine()!=null && !reference.getReferenceLine().isEmpty()) {
                        referenceLine = referenceLine + " " + reference.getReferenceLine();
                    }
                    if (reference.getDoi()!=null && !reference.getDoi().isEmpty()) {
                        referenceLine = referenceLine + " " + reference.getDoi();
                    }
                    referenceLine = referenceLine.trim();
                    refPubMedLine.appendChild(document.createTextNode(referenceLine));
                    additionalFields.appendChild(refPubMedLine);
                }
            }

            //Add submitter information
            if(project.getSubmitter() != null){
                Element submitter = document.createElement("field");
                submitter.setAttribute("name", "submitter");
                submitter.appendChild(document.createTextNode(getName(project.getSubmitter())));
                additionalFields.appendChild(submitter);

                Element submitterMail = document.createElement("field");
                submitterMail.setAttribute("name", "submitter_mail");
                submitterMail.appendChild(document.createTextNode(project.getSubmitter().getEmail()));
                additionalFields.appendChild(submitterMail);

                if(project.getSubmitter().getAffiliation() != null){
                    Element submitterAffiliation = document.createElement("field");
                    submitterAffiliation.setAttribute("name", "submitter_affiliation");
                    submitterAffiliation.appendChild(document.createTextNode(project.getSubmitter().getAffiliation()));
                    additionalFields.appendChild(submitterAffiliation);
                }
            }

            //Add LabHead information
            if(project.getLabHeads() != null && !project.getLabHeads().isEmpty()){
                for(LabHead labhead: project.getLabHeads()){

                    Element submitter = document.createElement("field");
                    submitter.setAttribute("name", "labhead");
                    submitter.appendChild(document.createTextNode(getName(labhead)));
                    additionalFields.appendChild(submitter);

                    Element submitterMail = document.createElement("field");
                    submitterMail.setAttribute("name", "labhead_mail");
                    submitterMail.appendChild(document.createTextNode(labhead.getEmail()));
                    additionalFields.appendChild(submitterMail);

                    if(labhead.getAffiliation() != null){
                        Element submitterAffiliation = document.createElement("field");
                        submitterAffiliation.setAttribute("name", "labhead_affiliation");
                        submitterAffiliation.appendChild(document.createTextNode(labhead.getAffiliation()));
                        additionalFields.appendChild(submitterAffiliation);
                    }
                }
            }

            //Add original link to the files
            if(submission.getDataFiles() != null && !submission.getDataFiles().isEmpty()){
                for(DataFile file: submission.getDataFiles()){
                    Element dataset_link = document.createElement("field");
                    dataset_link.setAttribute("name", "dataset_file");
                    String url;
                    if (fromPride) {
                        Date pubDate = project.getPublicationDate();
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(pubDate);
                        int month = calendar.get(Calendar.MONTH) + 1; // the month are zero based, hence the correction +1
                        int year = calendar.get(Calendar.YEAR);
                        url = "ftp://ftp.pride.ebi.ac.uk/pride/data/archive/" + year + "/" +  (month < 10 ? "0" : "") + month + "/"
                                + project.getAccession() + "/" + file.getFileName();
                    } else if (file.getUrl() != null && !file.getUrl().toString().isEmpty()) {
                        url = file.getUrl().toString();
                    } else {
                        url = NOT_AVAILABLE;
                    }
                    dataset_link.appendChild(document.createTextNode(url));
                    additionalFields.appendChild(dataset_link);
                }
            }

            entries.appendChild(entry);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            DOMSource source = new DOMSource(document);
            File outputXML = new File(outputDirectory,  "PRIDE_EBEYE_" + project.getAccession() + ".xml");
            StreamResult result = new StreamResult(outputXML.toURI().getPath());
            transformer.transform(source, result);
            logger.info("Finished generating EB-eye XML file for: " + outputDirectory + File.separator + "PRIDE_EBEYE_" + project.getAccession() + ".xml" );
        }

    }

    private String getName(User submitter) {
        if(submitter.getLastName() != null && submitter.getLastName().length() > 0)
            return submitter.getFirstName() + " " + submitter.getLastName();
        return submitter.getFirstName();
    }

    private String getName(LabHead submitter) {
        if(submitter.getLastName() != null && submitter.getLastName().length() > 0)
            return submitter.getFirstName() + " " + submitter.getLastName();
        return submitter.getFirstName();
    }

    /**
     * Sets the current project.
     * @param project   New project to be assigned.
     */
    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * Sets the current submission
     * @param submission    New submission to be assigned.
     */
    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    /**
     * Sets the current output directory.
     * @param outputDirectory   New output directory to be assigned.
     */
    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Sets the current proteins set.
     * @param proteins  New set of proteins.
     */
    public void setProteins(HashMap<String, String> proteins) {
        this.proteins = proteins;
    }

}
