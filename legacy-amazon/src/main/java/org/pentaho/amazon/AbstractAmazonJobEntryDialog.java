/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.amazon;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.ui.core.database.dialog.tags.ExtTextbox;
import org.pentaho.di.ui.job.entry.JobEntryDialog;
import org.pentaho.di.ui.spoon.XulSpoonSettingsManager;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.XulException;
import org.pentaho.ui.xul.XulRunner;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.binding.DefaultBindingFactory;
import org.pentaho.ui.xul.containers.XulDialog;
import org.pentaho.ui.xul.swt.SwtXulLoader;
import org.pentaho.ui.xul.swt.SwtXulRunner;

import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;

/**
 * Base functionality for a XUL-based Amazon job entry dialog
 */
public abstract class AbstractAmazonJobEntryDialog<E extends AbstractAmazonJobEntry> extends JobEntryDialog
  implements JobEntryDialogInterface {


  protected ResourceBundle bundle = new ResourceBundle() {
    @Override
    protected Object handleGetObject( String key ) {
      return BaseMessages.getString( getMessagesClass(), key );
    }

    @Override
    public Enumeration<String> getKeys() {
      // getKeys method not used
      return Collections.emptyEnumeration();
    }
  };

  private AbstractAmazonJobExecutorController controller;

  @SuppressWarnings( "unchecked" )
  protected AbstractAmazonJobEntryDialog( Shell parent, JobEntryInterface jobEntry, Repository rep, JobMeta jobMeta )
    throws XulException {
    super( parent, jobEntry, rep, jobMeta );
    init( (E) jobEntry );
  }

  /**
   * @return the name of the class to use to look up localized messages
   */
  protected abstract Class<?> getMessagesClass();

  /**
   * @return the file name for the XUL document to load for this dialog
   */
  protected abstract String getXulFile();

  /**
   * Create the controller for this dialog
   *
   * @param container      XUL DOM container loaded from the file path returned by {@link #getXulFile()}
   * @param jobEntry       Job entry this dialog supports
   * @param bindingFactory Binding factory to create bindings with
   * @return Controller capable of handling requests for this dialog
   */
  protected abstract AbstractAmazonJobExecutorController createController( XulDomContainer container,
                                                                           AbstractAmazonJobEntry jobEntry,
                                                                           BindingFactory bindingFactory );

  /**
   * Initialize this dialog for the job entry instance provided.
   *
   * @param jobEntry The job entry this dialog supports.
   */
  protected void init( AbstractAmazonJobEntry jobEntry ) throws XulException {
    SwtXulLoader swtXulLoader = new SwtXulLoader();
    // Register the settings manager so dialog position and size is restored
    swtXulLoader.setSettingsManager( XulSpoonSettingsManager.getInstance() );
    swtXulLoader.registerClassLoader( getClass().getClassLoader() );
    // Register Kettle's variable text box so we can reference it from XUL
    swtXulLoader.register( "VARIABLETEXTBOX", ExtTextbox.class.getName() );
    swtXulLoader.setOuterContext( shell );

    // Load the XUL document with the dialog defined in it
    XulDomContainer container = swtXulLoader.loadXul( getXulFile(), bundle );

    // Create the controller with a default binding factory for the document we just loaded
    BindingFactory bf = new DefaultBindingFactory();
    bf.setDocument( container.getDocumentRoot() );
    controller = createController( container, jobEntry, bf );
    container.addEventHandler( controller );
    setDialogSize();

    // Load up the SWT-XUL runtime and initialize it with our container
    final XulRunner runner = new SwtXulRunner();
    runner.addContainer( container );
    runner.initialize();
  }

  @Override
  public JobEntryInterface open() {
    return controller.open();
  }

  private void setDialogSize() {
    XulSpoonSettingsManager settingsManager = XulSpoonSettingsManager.getInstance();
    XulDialog dialog = controller.getDialog();

    if ( Const.isWindows() ) {
      settingsManager.storeSetting( controller.getDialogElementId() + ".Width", String.valueOf( dialog.getWidth() ) );
      settingsManager.storeSetting( controller.getDialogElementId() + ".Height", String.valueOf( dialog.getHeight() ) );
    } else {
      settingsManager.storeSetting( controller.getDialogElementId() + ".Width",
        String.valueOf( dialog.getAttributeValue( "linuxWidth" ) ) );
      settingsManager.storeSetting( controller.getDialogElementId() + ".Height",
        String.valueOf( dialog.getAttributeValue( "linuxHeight" ) ) );
    }
  }
}
