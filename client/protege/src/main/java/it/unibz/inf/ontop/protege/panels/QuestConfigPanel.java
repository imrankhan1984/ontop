package it.unibz.inf.ontop.protege.panels;

/*
 * #%L
 * ontop-protege4
 * %%
 * Copyright (C) 2009 - 2013 KRDB Research Centre. Free University of Bozen Bolzano.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import it.unibz.inf.ontop.injection.OntopMappingSettings;
import it.unibz.inf.ontop.injection.OntopOBDASettings;
import it.unibz.inf.ontop.injection.OntopReformulationSettings;
import it.unibz.inf.ontop.protege.core.DisposableProperties;

import java.awt.*;

public class QuestConfigPanel extends javax.swing.JPanel {

    private static final long serialVersionUID = 602382682995021070L;

    private final DisposableProperties preference;

    public QuestConfigPanel(DisposableProperties preference) {
        this.preference = preference;

        initComponents();
        setMaximumSize(new Dimension(1024,768));
        setMinimumSize(new Dimension(1024,768));

        chkRewrite.setSelected(preference.getBoolean(OntopReformulationSettings.EXISTENTIAL_REASONING));
        chkAnnotations.setSelected(preference.getBoolean(OntopMappingSettings.QUERY_ONTOLOGY_ANNOTATIONS));
        chkSameAs.setSelected(preference.getBoolean(OntopOBDASettings.SAME_AS));
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        mappingMode = new javax.swing.ButtonGroup();
        mapper = new javax.swing.ButtonGroup();
        datalocationGroup = new javax.swing.ButtonGroup();
        AboxMode = new javax.swing.ButtonGroup();
        labelNote = new javax.swing.JLabel();
        pnlReformulationMethods = new javax.swing.JPanel();
        chkRewrite = new javax.swing.JCheckBox();
        chkAnnotations = new javax.swing.JCheckBox();
        chkSameAs = new javax.swing.JCheckBox();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));

        setMinimumSize(new java.awt.Dimension(620, 300));
        setPreferredSize(new java.awt.Dimension(620, 300));
        setLayout(new java.awt.GridBagLayout());

        labelNote.setText("<html><b>Note:</b> You will need to restart Ontop Reasoner for any changes to take effect.<p/>&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; (i.e., select \"Reasoner-> None\" and then \"Reasoner -> Ontop\" in Protege's menu)</html>");
        labelNote.setAlignmentX(0.5F);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(labelNote, gridBagConstraints);

        pnlReformulationMethods.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.lightGray), "First Order reformulation"));
        pnlReformulationMethods.setMinimumSize(new java.awt.Dimension(620, 120));
        pnlReformulationMethods.setPreferredSize(new java.awt.Dimension(620, 120));
        pnlReformulationMethods.setLayout(new java.awt.GridBagLayout());

        chkRewrite.setText("Enable reasoning over anonymous individuals (tree-witness rewriting) ");
        chkRewrite.setToolTipText("Enable only if your application requires reasoning w.r.t. to existential constants in the queries");
        chkRewrite.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        chkRewrite.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkRewriteActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 3, 5);
        pnlReformulationMethods.add(chkRewrite, gridBagConstraints);

        chkAnnotations.setText("Enable querying annotations in the ontology");
        chkAnnotations.setToolTipText("Enable only if your application requires querying annotation properties defined in the ontology.");
        chkAnnotations.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        chkAnnotations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkAnnotationsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 3, 5);
        pnlReformulationMethods.add(chkAnnotations, gridBagConstraints);

        chkSameAs.setText("Enable reasoning with owl:sameAs from mappings");
        chkSameAs.setToolTipText("Enable only if your application requires reasoning with owl:sameAs from mappings");
        chkSameAs.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        chkSameAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkSameAsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 3, 5);
        pnlReformulationMethods.add(chkSameAs, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 2.0;
        pnlReformulationMethods.add(filler2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(pnlReformulationMethods, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(filler1, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void chkRewriteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkRewriteActionPerformed
        preference.put(OntopReformulationSettings.EXISTENTIAL_REASONING, String.valueOf(chkRewrite.isSelected()));
    }//GEN-LAST:event_chkRewriteActionPerformed

    private void chkAnnotationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkAnnotationsActionPerformed
        preference.put(OntopMappingSettings.QUERY_ONTOLOGY_ANNOTATIONS, String.valueOf(chkAnnotations.isSelected()));
    }//GEN-LAST:event_chkAnnotationsActionPerformed

    private void chkSameAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkSameAsActionPerformed
        preference.put(OntopOBDASettings.SAME_AS, String.valueOf(chkSameAs.isSelected()));
    }//GEN-LAST:event_chkSameAsActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup AboxMode;
    private javax.swing.JCheckBox chkAnnotations;
    private javax.swing.JCheckBox chkRewrite;
    private javax.swing.JCheckBox chkSameAs;
    private javax.swing.ButtonGroup datalocationGroup;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JLabel labelNote;
    private javax.swing.ButtonGroup mapper;
    private javax.swing.ButtonGroup mappingMode;
    private javax.swing.JPanel pnlReformulationMethods;
    // End of variables declaration//GEN-END:variables
}
