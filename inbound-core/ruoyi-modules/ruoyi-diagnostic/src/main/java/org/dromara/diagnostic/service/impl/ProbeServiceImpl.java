package org.dromara.diagnostic.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.diagnostic.config.ProbeProperties;
import org.dromara.diagnostic.domain.DiagnosticRun;
import org.dromara.diagnostic.domain.PlatformAdapter;
import org.dromara.diagnostic.domain.ProbeNode;
import org.dromara.diagnostic.domain.ProbeTask;
import org.dromara.diagnostic.domain.QuestionBank;
import org.dromara.diagnostic.domain.bo.ProbeCallbackBo;
import org.dromara.diagnostic.domain.bo.ProbeExtensionResultBo;
import org.dromara.diagnostic.domain.bo.ProbeNodeRegisterBo;
import org.dromara.diagnostic.domain.vo.PlatformAdapterVo;
import org.dromara.diagnostic.domain.vo.ProbeNodeVo;
import org.dromara.diagnostic.domain.vo.ProbePollTaskVo;
import org.dromara.diagnostic.mapper.DiagnosticRunMapper;
import org.dromara.diagnostic.mapper.PlatformAdapterMapper;
import org.dromara.diagnostic.mapper.ProbeNodeMapper;
import org.dromara.diagnostic.mapper.ProbeTaskMapper;
import org.dromara.diagnostic.mapper.QuestionBankMapper;
import org.dromara.diagnostic.service.IDiagnosticRunService;
import org.dromara.diagnostic.service.IProbeService;
import org.dromara.diagnostic.support.BusinessTenantHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProbeServiceImpl implements IProbeService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_DISPATCHED = "DISPATCHED";
    private static final String PROBE_MODE_EXTENSION = "browser-extension";

    private final ProbeNodeMapper probeNodeMapper;
    private final PlatformAdapterMapper platformAdapterMapper;
    private final ProbeTaskMapper probeTaskMapper;
    private final QuestionBankMapper questionBankMapper;
    private final DiagnosticRunMapper diagnosticRunMapper;
    private final IDiagnosticRunService diagnosticRunService;
    private final ProbeProperties probeProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerNode(ProbeNodeRegisterBo bo, String nodeKeyHeader) {
        String nodeKey = resolveNodeKey(bo.getNodeKey(), nodeKeyHeader);
        validateAllowedNodeKey(nodeKey);

        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        OffsetDateTime now = OffsetDateTime.now();

        ProbeNode existing = probeNodeMapper.selectOne(
            Wrappers.lambdaQuery(ProbeNode.class)
                .eq(ProbeNode::getTenantId, tenantId)
                .eq(ProbeNode::getNodeKey, nodeKey)
                .isNull(ProbeNode::getDeletedAt)
                .last("LIMIT 1")
        );

        List<String> platforms = bo.getPlatforms() != null ? bo.getPlatforms() : Collections.emptyList();

        if (existing == null) {
            ProbeNode node = new ProbeNode();
            node.setTenantId(tenantId);
            node.setNodeKey(nodeKey);
            node.setRegion(bo.getRegion());
            node.setPlatforms(platforms);
            node.setExtensionVersion(bo.getExtensionVersion());
            node.setStatus(STATUS_ACTIVE);
            node.setRateLimitJson(new HashMap<>());
            node.setLastHeartbeatAt(now);
            node.setCreatedAt(now);
            node.setUpdatedAt(now);
            probeNodeMapper.insert(node);
            return node.getId();
        }

        existing.setRegion(bo.getRegion());
        existing.setPlatforms(platforms);
        existing.setExtensionVersion(bo.getExtensionVersion());
        existing.setLastHeartbeatAt(now);
        existing.setUpdatedAt(now);
        probeNodeMapper.updateById(existing);
        return existing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProbePollTaskVo pollTask(String platform, String nodeKeyHeader) {
        if (StringUtils.isBlank(platform)) {
            throw new ServiceException("platform 不能为空", HttpStatus.BAD_REQUEST);
        }
        ProbeNode node = resolveNode(nodeKeyHeader);

        ProbeTask task = probeTaskMapper.selectOne(
            Wrappers.lambdaQuery(ProbeTask.class)
                .eq(ProbeTask::getTenantId, node.getTenantId())
                .eq(ProbeTask::getPlatform, platform)
                .eq(ProbeTask::getProbeMode, PROBE_MODE_EXTENSION)
                .apply("status = 'PENDING'::probe_task_status")
                .isNull(ProbeTask::getDeletedAt)
                .orderByAsc(ProbeTask::getId)
                .last("LIMIT 1 FOR UPDATE")
        );
        if (task == null) {
            return null;
        }

        OffsetDateTime now = OffsetDateTime.now();
        task.setStatus(STATUS_DISPATCHED);
        task.setProbeNodeId(node.getId());
        task.setDispatchedAt(now);
        task.setUpdatedAt(now);
        probeTaskMapper.updateById(task);

        QuestionBank question = questionBankMapper.selectById(task.getQuestionId());
        DiagnosticRun run = diagnosticRunMapper.selectById(task.getRunId());

        ProbePollTaskVo vo = new ProbePollTaskVo();
        vo.setProbeTaskId(task.getId());
        vo.setRunId(task.getRunId());
        vo.setQuestionId(task.getQuestionId());
        vo.setQuestion(question != null ? question.getQuestion() : null);
        vo.setPlatform(task.getPlatform());
        vo.setProbeMode(task.getProbeMode());
        if (run != null) {
            vo.setRegion(run.getRegion());
            vo.setLocale(run.getLocale());
            vo.setMarket(run.getMarket());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitResult(Long probeTaskId, ProbeExtensionResultBo bo, String nodeKeyHeader) {
        ProbeNode node = resolveNode(nodeKeyHeader);
        ProbeTask task = probeTaskMapper.selectById(probeTaskId);
        if (task == null || task.getDeletedAt() != null) {
            throw new ServiceException("探针任务不存在", HttpStatus.NOT_FOUND);
        }
        if (!node.getTenantId().equals(task.getTenantId())) {
            throw new ServiceException("无权操作该探针任务", HttpStatus.FORBIDDEN);
        }
        if (!PROBE_MODE_EXTENSION.equals(task.getProbeMode())) {
            throw new ServiceException("非 browser-extension 任务", HttpStatus.BAD_REQUEST);
        }
        if (task.getProbeNodeId() != null && !task.getProbeNodeId().equals(node.getId())) {
            throw new ServiceException("探针任务未分配给当前节点", HttpStatus.FORBIDDEN);
        }
        if (!STATUS_DISPATCHED.equals(task.getStatus()) && !"RUNNING".equals(task.getStatus())) {
            throw new ServiceException("探针任务状态不允许上报: " + task.getStatus(), HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> result = bo.getResult() != null ? new HashMap<>(bo.getResult()) : new HashMap<>();
        result.putIfAbsent("platform", task.getPlatform());
        result.putIfAbsent("probe_mode", PROBE_MODE_EXTENSION);
        result.putIfAbsent("capture_method", PROBE_MODE_EXTENSION);

        ProbeCallbackBo callback = new ProbeCallbackBo();
        callback.setProbeTaskId(probeTaskId);
        callback.setStatus(bo.getStatus());
        callback.setResult(result);
        callback.setErrorMessage(bo.getErrorMessage());
        diagnosticRunService.handleProbeCallback(callback);
    }

    @Override
    public List<PlatformAdapterVo> listAdapters(String nodeKeyHeader) {
        ProbeNode node = resolveNode(nodeKeyHeader);
        List<PlatformAdapter> rows = platformAdapterMapper.selectList(
            Wrappers.lambdaQuery(PlatformAdapter.class)
                .eq(PlatformAdapter::getEnabled, true)
                .isNull(PlatformAdapter::getDeletedAt)
                .and(w -> w.eq(PlatformAdapter::getTenantId, node.getTenantId()).or().isNull(PlatformAdapter::getTenantId))
        );
        List<PlatformAdapterVo> list = new ArrayList<>(rows.size());
        for (PlatformAdapter row : rows) {
            PlatformAdapterVo vo = new PlatformAdapterVo();
            vo.setPlatform(row.getPlatform());
            vo.setVersion(row.getVersion());
            vo.setDomSelectors(row.getDomSelectorsJson());
            vo.setApiPatterns(row.getApiPatternsJson());
            vo.setParseRules(row.getParseRulesJson());
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<ProbeNodeVo> listNodesForCurrentTenant() {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        List<ProbeNode> nodes = probeNodeMapper.selectList(
            Wrappers.lambdaQuery(ProbeNode.class)
                .eq(ProbeNode::getTenantId, tenantId)
                .isNull(ProbeNode::getDeletedAt)
                .orderByDesc(ProbeNode::getLastHeartbeatAt)
        );
        OffsetDateTime onlineCutoff = OffsetDateTime.now().minusSeconds(probeProperties.getOnlineWithinSeconds());
        List<ProbeNodeVo> list = new ArrayList<>(nodes.size());
        for (ProbeNode node : nodes) {
            ProbeNodeVo vo = new ProbeNodeVo();
            vo.setId(node.getId());
            vo.setNodeKey(node.getNodeKey());
            vo.setRegion(node.getRegion());
            vo.setPlatforms(node.getPlatforms());
            vo.setExtensionVersion(node.getExtensionVersion());
            vo.setStatus(node.getStatus());
            vo.setLastHeartbeatAt(node.getLastHeartbeatAt());
            vo.setOnline(node.getLastHeartbeatAt() != null && !node.getLastHeartbeatAt().isBefore(onlineCutoff));
            list.add(vo);
        }
        return list;
    }

    private ProbeNode resolveNode(String nodeKeyHeader) {
        String nodeKey = StringUtils.trim(nodeKeyHeader);
        if (StringUtils.isBlank(nodeKey)) {
            throw new ServiceException("缺少 X-Probe-Node-Key", HttpStatus.UNAUTHORIZED);
        }
        validateAllowedNodeKey(nodeKey);

        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        ProbeNode node = probeNodeMapper.selectOne(
            Wrappers.lambdaQuery(ProbeNode.class)
                .eq(ProbeNode::getTenantId, tenantId)
                .eq(ProbeNode::getNodeKey, nodeKey)
                .isNull(ProbeNode::getDeletedAt)
                .last("LIMIT 1")
        );
        if (node == null) {
            throw new ServiceException("探针节点未注册: " + nodeKey, HttpStatus.UNAUTHORIZED);
        }
        return node;
    }

    private String resolveNodeKey(String bodyKey, String headerKey) {
        String key = StringUtils.isNotBlank(bodyKey) ? bodyKey.trim() : StringUtils.trim(headerKey);
        if (StringUtils.isBlank(key)) {
            throw new ServiceException("nodeKey 不能为空", HttpStatus.BAD_REQUEST);
        }
        return key;
    }

    private void validateAllowedNodeKey(String nodeKey) {
        List<String> allowed = probeProperties.getAllowedNodeKeys();
        if (allowed == null || allowed.isEmpty()) {
            return;
        }
        if (!allowed.contains(nodeKey)) {
            throw new ServiceException("探针 node_key 未授权", HttpStatus.UNAUTHORIZED);
        }
    }
}
